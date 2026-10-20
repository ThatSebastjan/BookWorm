import { ConflictException, Injectable, UnauthorizedException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { User, UserType } from './schema/user.schema.js';
import { Model } from 'mongoose';
import bcrypt from 'bcrypt';
import { ConfigService } from '@nestjs/config';
import { TokenPair, TokenService } from '@nestjs/authentication';
import { LoginUserDto, RegisterUserDto } from './dto/auth.dto.js';

@Injectable()
export class AuthService {

  saltRounds: number;

  constructor(
    @InjectModel(User.name) private userModel: Model<User>,
    private configService: ConfigService,
    private readonly tokenService: TokenService,
  ) {
    this.saltRounds = Number(this.configService.get<number>("BCRYPT_SALT_ROUNDS")!);
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


  async register(registerUserDto: RegisterUserDto) {
    
    const existingUser = await this.userModel.findOne({ username: registerUserDto.username });

    if(existingUser != null){
      throw new ConflictException('Username already taken');
    };

    const salt = await bcrypt.genSalt(this.saltRounds);
    const passwordHash = await bcrypt.hash(registerUserDto.password, salt);

    const newUser = new this.userModel({
      username: registerUserDto.username,
      password: passwordHash,
      role: registerUserDto.role,
    });

    await newUser.save();
  }


  async validateLogin(username: string, password: string): Promise<UserType | null> {
    const user = await this.userModel.findOne({ username: username });

    if(user == null){
      return null;
    }

    const passwordValid = await bcrypt.compare(password, user.password);

    if(!passwordValid){
      return null;
    }

    return user;
  }


  async login(loginUserDto: LoginUserDto) {
    const user = await this.validateLogin(loginUserDto.username, loginUserDto.password);

    if(user == null){
      throw new UnauthorizedException("Invalid username or password");
    };

    return await this.issueToken(user);
  }
}
    