import {
  ConflictException,
  Injectable,
  UnauthorizedException,
} from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { User, UserType } from './schema/user.schema.js';
import { Profile } from '../profile/schema/profile.schema.js';
import { Model } from 'mongoose';
import bcrypt from 'bcrypt';
import { ConfigService } from '@nestjs/config';
import { TokenPair, TokenService } from '@nestjs/authentication';
import { LoginUserDto, RegisterUserDto } from './dto/auth.dto.js';
import { Role } from '../constants/api.enums.js';

const DUPLICATE_KEY = 11000;

@Injectable()
export class AuthService {
  saltRounds: number;

  constructor(
    @InjectModel(User.name) private userModel: Model<User>,
    @InjectModel(Profile.name) private profileModel: Model<Profile>,
    private configService: ConfigService,
    private readonly tokenService: TokenService,
  ) {
    this.saltRounds = Number(
      this.configService.get<number>('BCRYPT_SALT_ROUNDS') ?? 10,
    );
  }

  async issueToken(user: UserType) {
    return this.tokenService.issue(user._id.toString(), {
      method: 'password',
      claims: { amr: ['pwd'] },
    });
  }

  async refreshToken(refreshToken: string): Promise<TokenPair> {
    return this.tokenService.refresh(refreshToken);
  }

  /** Creates the user and their profile, then signs them in. */
  async register(registerUserDto: RegisterUserDto, role: Role = Role.STUDENT) {
    const { name, surname, email, password } = registerUserDto;

    if (await this.userModel.exists({ email })) {
      throw new ConflictException('Email already registered');
    }

    const salt = await bcrypt.genSalt(this.saltRounds);
    const passwordHash = await bcrypt.hash(password, salt);

    let user: UserType;
    try {
      user = await this.userModel.create({
        email,
        password: passwordHash,
        role,
      });
    } catch (error) {
      // Two sign-ups with the same email at once: the unique index catches the second.
      if ((error as { code?: number }).code === DUPLICATE_KEY) {
        throw new ConflictException('Email already registered');
      }
      throw error;
    }

    try {
      const profile = await this.profileModel.create({
        user: user._id,
        name,
        surname,
      });
      user.profile = profile._id;
      await user.save();
    } catch (error) {
      // No transactions without a replica set, so undo by hand: never leave a user without a profile.
      await this.profileModel.deleteOne({ user: user._id });
      await this.userModel.deleteOne({ _id: user._id });
      throw error;
    }

    return this.issueToken(user);
  }

  async validateLogin(
    email: string,
    password: string,
  ): Promise<UserType | null> {
    const user = await this.userModel.findOne({ email });

    if (user == null) {
      return null;
    }

    const passwordValid = await bcrypt.compare(password, user.password);

    if (!passwordValid) {
      return null;
    }

    return user;
  }

  async login(loginUserDto: LoginUserDto) {
    const user = await this.validateLogin(
      loginUserDto.email,
      loginUserDto.password,
    );

    if (user == null) {
      throw new UnauthorizedException('Invalid email or password');
    }

    return await this.issueToken(user);
  }
}
