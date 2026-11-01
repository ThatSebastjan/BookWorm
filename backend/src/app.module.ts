import { Module } from '@nestjs/common';
import { AppController } from './app.controller.js';
import { AppService } from './app.service.js';
import { MongooseModule } from '@nestjs/mongoose';
import { AuthModule } from './auth/auth.module.js';
import { ConfigModule, ConfigService } from '@nestjs/config';
import databaseConfig, {
  DATABASE_CONFIG_KEY,
} from './config/database.config.js';
import { AuthenticationModule } from '@nestjs/authentication';
import { UserModule } from './user/user.module.js';
import { ClassroomModule } from './classroom/classroom.module.js';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      load: [databaseConfig],
    }),

    MongooseModule.forRootAsync({
      imports: [ConfigModule],
      useFactory: async (configService: ConfigService) => ({
        uri: configService.get<string>(`${DATABASE_CONFIG_KEY}.uri`),
      }),
      inject: [ConfigService],
    }),

    AuthenticationModule.forRootAsync({
      useFactory: () => ({
        accessToken: {
          key: process.env.JWT_SECRET!,
          ttl: '60m', // TODO: make this a config option?
        },
      }),
    }),

    AuthModule,
    UserModule,
    ClassroomModule,
  ],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}
