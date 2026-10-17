import { Body, Controller, Post } from '@nestjs/common';
import { AuthService } from './auth.service.js';
import { Public } from '@nestjs/authentication';
import { LoginUserDto, RefreshTokenDto, RegisterUserDto } from './dto/auth.dto.js';


@Public()
@Controller('auth')
export class AuthController {
  constructor(private readonly authService: AuthService) {}


  @Post('register')
  async register(@Body() registerUserDto: RegisterUserDto) {
    return this.authService.register(registerUserDto);
  }


  @Post('login')
  async login(@Body() loginUserDto: LoginUserDto) {
    return this.authService.login(loginUserDto);
  }


  @Post('refresh')
  async refreshToken(@Body() refreshTokenDto: RefreshTokenDto) {
    return this.authService.refreshToken(refreshTokenDto.refreshToken);
  }
}         
