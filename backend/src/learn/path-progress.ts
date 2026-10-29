import mongoose from 'mongoose';
import { LessonStatus } from './dto/learn.dto.js';
import { UnlockMode } from '../learning-path/schema/learning-path.schema.js';

interface UnitLessons {
  lessons: { _id: mongoose.Types.ObjectId }[];
}

/**
 * Done lessons stay done; the rest open one at a time, along the whole path ([UnlockMode.IN_ORDER])
 * or inside each chapter ([UnlockMode.ANY_CHAPTER]). Returns the status of each lesson by id.
 */
export function lessonStatuses(unlockMode: UnlockMode, units: UnitLessons[], done: Set<string>): Map<string, LessonStatus> {
  const statuses = new Map<string, LessonStatus>();
  let opened = false;
  for (const unit of units) {
    if (unlockMode === UnlockMode.ANY_CHAPTER) opened = false;
    for (const lesson of unit.lessons) {
      const id = lesson._id.toString();
      if (done.has(id)) {
        statuses.set(id, LessonStatus.DONE);
      } else if (!opened) {
        statuses.set(id, LessonStatus.CURRENT);
        opened = true;
      } else {
        statuses.set(id, LessonStatus.LOCKED);
      }
    }
  }
  return statuses;
}

/** 1-based position of the first chapter with a lesson left to do; the last one when all are done. */
export function currentChapter(units: UnitLessons[], done: Set<string>): number {
  const index = units.findIndex((unit) => unit.lessons.some((lesson) => !done.has(lesson._id.toString())));
  return index >= 0 ? index + 1 : units.length;
}

/** The calendar day of [date] on the server, as YYYY-MM-DD. */
export function dayKey(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function addDays(date: Date, days: number): Date {
  const copy = new Date(date);
  copy.setDate(copy.getDate() + days);
  return copy;
}

/** Days in a row with practice, ending today, or yesterday while today has none yet. */
export function streak(days: Set<string>, today = new Date()): number {
  let day = days.has(dayKey(today)) ? today : addDays(today, -1);
  let count = 0;
  while (days.has(dayKey(day))) {
    count++;
    day = addDays(day, -1);
  }
  return count;
}

/** The longest run of days in a row with practice. */
export function longestStreak(days: Set<string>): number {
  let longest = 0;
  for (const key of days) {
    const [y, m, d] = key.split('-').map(Number);
    const start = new Date(y, m - 1, d);
    // Only count runs from their first day.
    if (days.has(dayKey(addDays(start, -1)))) continue;
    let length = 0;
    while (days.has(dayKey(addDays(start, length)))) length++;
    longest = Math.max(longest, length);
  }
  return longest;
}

/** Monday to Sunday of this week. */
export function thisWeek(today = new Date()): Date[] {
  const monday = addDays(today, -((today.getDay() + 6) % 7));
  return Array.from({ length: 7 }, (_, i) => addDays(monday, i));
}
