import { Body, Controller, HttpCode, HttpStatus, Post } from '@nestjs/common';
import { AuthService } from './auth.service.js';
import { Public } from '@nestjs/authentication';
import {
  LoginUserDto,
  RefreshTokenDto,
  RegisterUserDto,
} from './dto/auth.dto.js';
import { ApiOperation, ApiResponse } from '@nestjs/swagger';

@Public()
@Controller('auth')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  @Post('register')
  @ApiOperation({ summary: 'Register a new user' })
  @ApiResponse({
    status: 201,
    description: 'User successfully created',
  })
  @ApiResponse({
    status: 409,
    description: 'Username already taken',
  })
  async register(@Body() registerUserDto: RegisterUserDto) {
    return this.authService.register(registerUserDto);
  }

  @Post('login')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({
    summary: 'Login with username and password and get JWT token for auth',
  })
  @ApiResponse({
    status: 200,
    description:
      'Response when valid username and password are provided. Refresh token is one time use only!',
    schema: {
      example: {
        accessToken: 'ey...',
        expiresIn: 3600,
        refreshToken: 'XKpj...',
      },
    },
  })
  @ApiResponse({
    status: 401,
    description: 'Provided credentials are invalid',
  })
  async login(@Body() loginUserDto: LoginUserDto) {
    return this.authService.login(loginUserDto);
  }

  @Post('refresh')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({
    summary: 'Get a new access token using the returned refresh token',
  })
  @ApiResponse({
    status: 200,
    description: 'Response when valid refresh token is provided',
    schema: {
      example: {
        accessToken: 'ey...',
        expiresIn: 3600,
        refreshToken: 'XKpj...',
      },
    },
  })
  @ApiResponse({
    status: 401,
    description: 'Provided refresh token is reused or invalid',
  })
  async refreshToken(@Body() refreshTokenDto: RefreshTokenDto) {
    return this.authService.refreshToken(refreshTokenDto.refreshToken);
  }
}
