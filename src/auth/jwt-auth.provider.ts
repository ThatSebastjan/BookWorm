import { Injectable } from '@nestjs/common';
import { AuthenticationRegistry, JwtBearerProvider, type JwtClaims } from '@nestjs/authentication';
import { User } from './schema/user.schema.js';
import { UserService } from '../user/user.service.js';


@Injectable()
export class JwtAuth extends JwtBearerProvider<User> {
  constructor(
    private readonly userService: UserService,
    registry: AuthenticationRegistry,
  ) {
    super({ realm: 'store' });
    registry.registerProvider(this);
  }

  // Called after the signature and claims checked out
  validate({ sub }: JwtClaims) {
    return sub ? this.userService.findById(sub) : null;
  }
}
