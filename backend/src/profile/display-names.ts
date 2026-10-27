import mongoose, { Model } from 'mongoose';
import { Profile } from './schema/profile.schema.js';

/** "Name Surname" of each user, by user id. Users without a profile are left out. */
export async function displayNames(
  profileModel: Model<Profile>,
  userIds: mongoose.Types.ObjectId[],
): Promise<Map<string, string>> {
  const profiles = await profileModel.find({ user: { $in: userIds } });
  return new Map(profiles.map((p) => [p.user.toString(), `${p.name} ${p.surname}`]));
}
