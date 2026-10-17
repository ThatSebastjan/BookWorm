import { registerAs } from '@nestjs/config';

export const DATABASE_CONFIG_KEY = 'database';

export default registerAs(DATABASE_CONFIG_KEY, () => ({
  uri: process.env.DATABASE_URL,
}));