import { ConflictException, Injectable, UnauthorizedException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { User } from './schema/user.schema.js';
import { Model } from 'mongoose';
import { RegisterUserDto } from './dto/register.dto.js';
import bcrypt from 'bcrypt';
import { ConfigService } from '@nestjs/config';
import { LoginUserDto } from './dto/login.dto.js';

@Injectable()
export class AuthService {

  saltRounds: number;

  constructor(
    @InjectModel(User.name) private userModel: Model<User>,
    private configService: ConfigService
  ) {
    this.saltRounds = Number(this.configService.get<number>("BCRYPT_SALT_ROUNDS")!);
  }


  async register(registerUserDto: RegisterUserDto) {
    
    const existingUser = await this.userModel.findOne({ username: registerUserDto.username });

    if(existingUser != null){
      throw new ConflictException("Username already taken");
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


  async validateLogin(username: string, password: string): Promise<User | null> {
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

    return user; // TODO: return JWT
  }
}
    