import { IsNotEmpty, IsEnum } from "class-validator";
import { PasswordProperty } from "./password.decorator.js";
import { Role } from "../../constants/api.enums.js";


export class RegisterUserDto {
  @IsNotEmpty()
  username: string;

  @PasswordProperty()
  password: string;

  @IsEnum(Role)
  role: Role;
}


export class LoginUserDto {
  @IsNotEmpty()
  username: string;

  @PasswordProperty()
  password: string;
}


export class RefreshTokenDto {
    @IsNotEmpty()
    refreshToken: string;
}


