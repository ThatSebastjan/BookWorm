import { Module } from '@nestjs/common';
import { AuthService } from './auth.service.js';
import { AuthController } from './auth.controller.js';
import { MongooseModule } from '@nestjs/mongoose';
import { User, UserSchema } from './schema/user.schema.js';
import { Profile, ProfileSchema } from '../profile/schema/profile.schema.js';
import { JwtAuth } from './jwt-auth.provider.js';
import { UserService } from '../user/user.service.js';
import { MongoRefreshTokenStore } from './refresh-token/mongo-refresh-token.store.js';
import {
  RefreshToken,
  RefreshTokenRevocation,
  RefreshTokenRevocationSchema,
  RefreshTokenSchema,
} from './refresh-token/refresh-token.schema.js';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: User.name, schema: UserSchema },
      { name: Profile.name, schema: ProfileSchema },
      { name: RefreshToken.name, schema: RefreshTokenSchema },
      { name: RefreshTokenRevocation.name, schema: RefreshTokenRevocationSchema },
    ]),
  ],
  controllers: [AuthController],
  providers: [AuthService, JwtAuth, UserService, MongoRefreshTokenStore],
})
export class AuthModule {}
