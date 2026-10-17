import { IsNotEmpty } from "class-validator";
import { PasswordProperty } from "./password.decorator.js";


export class LoginUserDto {
  @IsNotEmpty()
  username: string;

  @PasswordProperty()
  password: string;
}