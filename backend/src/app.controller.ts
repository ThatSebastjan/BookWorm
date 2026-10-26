import { Controller, Get, HttpCode, HttpStatus } from '@nestjs/common';
import { AppService } from './app.service.js';
import { ApiOperation, ApiResponse } from '@nestjs/swagger';
import { Public } from '@nestjs/authentication';

@Controller()
export class AppController {
  constructor(private readonly appService: AppService) {}

  @Public()
  @Get('/status')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Check API status' })
  @ApiResponse({
    status: 200,
    description: 'API is running',
    schema: {
      example: {
        status: 'OK',
        timestamp: 1792575900000,
      },
    },
  })
  statusReport() {
    return {
      status: 'OK',
      server_time: Date.now(),
    };
  }
}
