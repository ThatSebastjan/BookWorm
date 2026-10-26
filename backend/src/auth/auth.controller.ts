import { Body, Controller, HttpCode, HttpStatus, Post } from '@nestjs/common';
import { AuthService } from './auth.service.js';
import { Public } from '@nestjs/authentication';
import {
  LoginUserDto,
  RefreshTokenDto,
  RegisterUserDto,
} from './dto/auth.dto.js';
import { ApiOperation, ApiResponse } from '@nestjs/swagger';
import { Role } from '../constants/api.enums.js';

@Public()
@Controller('auth')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  @Post('register')
  @ApiOperation({
    summary:
      'Register a new student with name, surname, email and password. Creates their profile and signs them in.',
  })
  @ApiResponse({
    status: 201,
    description: 'User and profile created. Returns tokens, like /login.',
    schema: {
      example: {
        accessToken: 'ey...',
        expiresIn: 3600,
        refreshToken: 'XKpj...',
      },
    },
  })
  @ApiResponse({
    status: 400,
    description: 'A field is missing or invalid (e.g. weak password)',
  })
  @ApiResponse({
    status: 409,
    description: 'Email already registered',
  })
  async register(@Body() registerUserDto: RegisterUserDto) {
    return this.authService.register(registerUserDto);
  }

  // Open to anyone for now. Before real schools use it, gate it (an invite or school code),
  // or a student could sign up here and get teacher rights.
  @Post('register/teacher')
  @ApiOperation({
    summary:
      'Register a new teacher (from the desktop app). Same fields and answers as /auth/register.',
  })
  @ApiResponse({
    status: 201,
    description: 'Teacher and profile created. Returns tokens, like /login.',
  })
  @ApiResponse({
    status: 400,
    description: 'A field is missing or invalid (e.g. weak password)',
  })
  @ApiResponse({
    status: 409,
    description: 'Email already registered',
  })
  async registerTeacher(@Body() registerUserDto: RegisterUserDto) {
    return this.authService.register(registerUserDto, Role.TEACHER);
  }

  @Post('login')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({
    summary: 'Login with email and password and get JWT token for auth',
  })
  @ApiResponse({
    status: 200,
    description:
      'Response when valid email and password are provided. Refresh token is one time use only!',
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
