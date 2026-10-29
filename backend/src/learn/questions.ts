/*
 * Lesson questions in the app's own JSON shape (see AuthoredQuestionJson in the app).
 * Teachers store them with answer keys; students get them without, and send answers back.
 */

export interface OptionJson {
  id: string;
  text: string;
  numerator?: number;
  denominator?: number;
}

/** One field is set, depending on the question type. */
export interface AnswerJson {
  choice?: string;
  choices?: string[];
  text?: string;
  ordered?: string[];
  pairs?: Record<string, string>;
}

/** A question as the teacher wrote it. [options] are the choices, the items to order, or a match's left column. */
export interface AuthoredQuestion {
  type: string;
  id: string;
  prompt: string;
  hint?: string | null;
  options?: OptionJson[];
  right?: OptionJson[];
  fractionInput?: boolean;
  textBefore?: string;
  textAfter?: string;
  words?: string[];
  answer: AnswerJson;
  explanation?: string;
  retryHint?: string;
}

/** What the server keeps of a served question to check the answer. */
export interface AnswerKey {
  id: string;
  answer: AnswerJson;
  explanation: string;
  retryHint: string;
}

export type StudentQuestion = Omit<AuthoredQuestion, 'answer' | 'explanation' | 'retryHint'>;

export function answerKey(question: AuthoredQuestion): AnswerKey {
  return {
    id: question.id,
    answer: question.answer ?? {},
    explanation: question.explanation ?? '',
    retryHint: question.retryHint ?? '',
  };
}

/**
 * The question without its answer. Order items and a match's right column are stored in
 * their correct order, so they are moved around first.
 */
export function forStudent(question: AuthoredQuestion): StudentQuestion {
  const { answer: _answer, explanation: _explanation, retryHint: _retryHint, ...rest } = question;
  if (question.type === 'order') return { ...rest, options: rotated(question.options ?? []) };
  if (question.type === 'match') return { ...rest, right: rotated(question.right ?? []) };
  return rest;
}

function rotated<T>(items: T[]): T[] {
  return items.length < 2 ? items : [...items.slice(1), items[0]];
}

/** Typed answers ignore case and spaces, so "3 / 4" counts as "3/4". */
function normalised(text: string | undefined): string {
  return (text ?? '').replace(/\s+/g, '').toLowerCase();
}

function sameList(a: string[] | undefined, b: string[] | undefined): boolean {
  return a != null && b != null && a.length === b.length && a.every((item, i) => item === b[i]);
}

export function isCorrect(key: AnswerJson, given: AnswerJson): boolean {
  if (key.choice != null) return given.choice === key.choice;
  if (key.choices != null) {
    return given.choices != null && sameList([...new Set(key.choices)].sort(), [...new Set(given.choices)].sort());
  }
  if (key.ordered != null) return sameList(key.ordered, given.ordered);
  if (key.pairs != null) {
    const expected = Object.entries(key.pairs);
    const pairs = given.pairs ?? {};
    return expected.length === Object.keys(pairs).length && expected.every(([left, right]) => pairs[left] === right);
  }
  return given.text != null && normalised(given.text) === normalised(key.text);
}

/** Texts of the practice questions made from flashcards, in the student's language. */
const practiceTexts = {
  en: {
    prompt: (term: string) => `What does “${term}” mean?`,
    retryHint: (term: string) => `Think back to the flashcard for “${term}”.`,
  },
  sl: {
    prompt: (term: string) => `Kaj pomeni „${term}“?`,
    retryHint: (term: string) => `Spomni se kartice „${term}“.`,
  },
};

/** The language of an Accept-Language header the practice texts exist in; English otherwise. */
export function practiceLanguage(acceptLanguage: string | undefined): keyof typeof practiceTexts {
  return acceptLanguage?.trim().toLowerCase().startsWith('sl') ? 'sl' : 'en';
}

/**
 * Practice for a lesson the teacher wrote no questions for: "What does X mean?" with up to
 * four meanings to choose from. The same as questionsFromFlashcards in the app.
 */
export function questionsFromFlashcards(
  cards: { id: string; term: string; definition: string }[],
  language: keyof typeof practiceTexts,
  max = 6,
): AuthoredQuestion[] {
  if (cards.length < 2) return [];
  const texts = practiceTexts[language];
  return cards.slice(0, max).map((card, index) => {
    const rest = cards.filter((other) => other.id !== card.id);
    const others = Array.from({ length: Math.min(3, rest.length) }, (_, i) => rest[(index + i) % rest.length]);
    const meanings = [...new Set([...others.map((c) => c.definition), card.definition])];
    // Put the right meaning in a different place each time.
    const options = meanings
      .map((_, i) => meanings[(i + index) % meanings.length])
      .map((text, i) => ({ id: String.fromCharCode(97 + i), text }));
    return {
      type: 'singleChoice',
      id: `fc-${card.id}`,
      prompt: texts.prompt(card.term),
      options,
      answer: { choice: options.find((o) => o.text === card.definition)!.id },
      explanation: `${card.term}: ${card.definition}`,
      retryHint: texts.retryHint(card.term),
    };
  });
}
