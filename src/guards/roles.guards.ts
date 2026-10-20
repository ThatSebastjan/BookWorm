import { CanActivate, ExecutionContext, ForbiddenException, Injectable } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { Role } from '../constants/api.enums.js';
import { ROLES_KEY } from './roles.decorator.js';


@Injectable()
export class RolesGuard implements CanActivate {

  constructor(private reflector: Reflector) {}


  canActivate(context: ExecutionContext): boolean {
    const requiredRoles = this.reflector.getAllAndOverride<Role[]>(ROLES_KEY, [
      context.getHandler(),
      context.getClass(),
    ]);

    if (!requiredRoles || requiredRoles.length === 0){
        return true;
    }

    const { user } = context.switchToHttp().getRequest();

    if (!user){
        throw new ForbiddenException('Not authenticated');
    }

    const userRoles: Role[] = Array.isArray(user.roles) ? user.roles : [user.role];

    const hasRole = requiredRoles.some((role) => userRoles.includes(role));

    if (!hasRole){
        throw new ForbiddenException('No permission');
    }

    return true;
  }
}