import { ApiProperty } from '@nestjs/swagger';
import { Role } from '../../constants/api.enums.js';

/** What `GET /user` returns: the user plus their profile, without the password. */
export class MeDto {
  @ApiProperty({ example: '6720f0c2a1b2c3d4e5f60718' })
  id: string;

  @ApiProperty({ example: 'mia@example.com' })
  email: string;

  @ApiProperty({ enum: Role })
  role: Role;

  @ApiProperty({ example: 'Mia' })
  name: string;

  @ApiProperty({ example: 'Novak' })
  surname: string;

  @ApiProperty()
  createdAt: Date;
}
