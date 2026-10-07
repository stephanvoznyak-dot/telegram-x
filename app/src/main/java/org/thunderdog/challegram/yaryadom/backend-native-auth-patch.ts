/**
 * Патч для apps/bot/src/index.ts
 * Добавляет безопасную поддержку нативного Android-клиента (Telegram X).
 *
 * ИНСТРУКЦИЯ:
 * 1. Добавьте в .env:
 *      NATIVE_CLIENT_SECRET=длинный-случайный-секрет-не-менее-32-символов
 * 2. Импортируйте crypto и используйте resolveUser вместо userFromInit.
 * 3. Замените схемы запросов на версии ниже (initData становится опциональным).
 *
 * Безопасность:
 * - При nativeClient=true требуется HMAC-SHA256 подпись.
 * - Окно времени подписи — 5 минут.
 * - Без секрета или при неверной подписи запрос отклоняется (401).
 */

import { createHmac, timingSafeEqual } from 'node:crypto';
import { z } from 'zod';
import { validateInitData, type TgUser } from './telegram-auth.js';

// ---------------------------------------------------------------------------
// 1. Расширенные схемы
// ---------------------------------------------------------------------------

const nativeUserFields = {
  nativeClient: z.literal(true).optional(),
  userId: z.number().int().positive().optional(),
  firstName: z.string().min(1).max(128).optional(),
  username: z.string().max(64).nullable().optional(),
  timestamp: z.number().int().positive().optional(),
  signature: z.string().min(64).max(128).optional(),
};

export const createSchema = z.object({
  initData: z.string().min(1).optional(),
  ...nativeUserFields,
  category: z.enum([
    'RIDE', 'DELIVERY', 'REPAIR', 'CLEANING', 'SHOPPING',
    'COMPUTER', 'HELP', 'RENTAL', 'OTHER',
  ]),
  description: z.string().min(3).max(500),
  latitude: z.number().min(-90).max(90),
  longitude: z.number().min(-180).max(180),
  destinationText: z.string().max(300).optional(),
  radiusMeters: z.number(),
  expiresInMinutes: z.number().int().min(5).max(1440).default(30),
});

export const nearbySchema = z.object({
  initData: z.string().min(1).optional(),
  ...nativeUserFields,
  latitude: z.number().min(-90).max(90),
  longitude: z.number().min(-180).max(180),
  radiusMeters: z.number().default(5000),
});

export const takeSchema = z.object({
  initData: z.string().min(1).optional(),
  ...nativeUserFields,
  orderId: z.string().uuid(),
});

export const orderIdSchema = z.object({
  initData: z.string().min(1).optional(),
  ...nativeUserFields,
  orderId: z.string().uuid(),
});

export const authSchema = z.object({
  initData: z.string().min(1).optional(),
  ...nativeUserFields,
});

// ---------------------------------------------------------------------------
// 2. Проверка HMAC
// ---------------------------------------------------------------------------

function verifyNativeSignature(
  userId: number,
  firstName: string,
  timestamp: number,
  signature: string,
  secret: string
): boolean {
  const now = Math.floor(Date.now() / 1000);
  // Окно ±5 минут
  if (Math.abs(now - timestamp) > 300) {
    return false;
  }

  const payload = `${userId}:${firstName}:${timestamp}`;
  const expected = createHmac('sha256', secret)
    .update(payload)
    .digest('hex');

  try {
    const a = Buffer.from(expected, 'utf8');
    const b = Buffer.from(signature, 'utf8');
    if (a.length !== b.length) return false;
    return timingSafeEqual(a, b);
  } catch {
    return false;
  }
}

// ---------------------------------------------------------------------------
// 3. Единый резолвер пользователя
// ---------------------------------------------------------------------------

/**
 * Разрешает пользователя из тела запроса.
 * @param body — распарсенное тело
 * @param botToken — TOKEN бота (для initData)
 * @param nativeSecret — NATIVE_CLIENT_SECRET из .env (обязателен для native)
 */
export function resolveUser(
  body: {
    initData?: string;
    nativeClient?: boolean;
    userId?: number;
    firstName?: string;
    username?: string | null;
    timestamp?: number;
    signature?: string;
  },
  botToken: string,
  nativeSecret?: string
): TgUser | null {
  // Путь Mini App (старый, проверенный)
  if (body.initData) {
    return validateInitData(body.initData, botToken);
  }

  // Путь нативного клиента (Telegram X / Android)
  if (
    body.nativeClient === true &&
    body.userId &&
    body.firstName &&
    body.timestamp &&
    body.signature
  ) {
    if (!nativeSecret) {
      // Секрет не настроен — отклоняем native-запросы
      return null;
    }

    const ok = verifyNativeSignature(
      body.userId,
      body.firstName,
      body.timestamp,
      body.signature,
      nativeSecret
    );

    if (!ok) return null;

    return {
      id: body.userId,
      first_name: body.firstName,
      username: body.username ?? undefined,
    };
  }

  return null;
}

// ---------------------------------------------------------------------------
// 4. Пример использования в хендлерах
// ---------------------------------------------------------------------------

/*
import { resolveUser, createSchema, nearbySchema, ... } from './native-auth.js';

// В каждом /api/* хендлере:

const parsed = createSchema.safeParse(request.body);
if (!parsed.success) {
  return reply.status(400).send({ error: 'BAD_REQUEST' });
}

const user = resolveUser(parsed.data, process.env.TOKEN!, process.env.NATIVE_CLIENT_SECRET);
if (!user) {
  return reply.status(401).send({ error: 'UNAUTHORIZED' });
}

// дальше работа с user.id, user.first_name и т.д.
*/
