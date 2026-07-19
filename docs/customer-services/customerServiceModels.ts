export type BookingMode = "FIXED" | "HOURLY";

export interface CustomerServiceCard {
  id?: number;
  name?: string;
  serviceId: number;
  serviceName: string;
  description?: string;
  imageUrl?: string;
  price?: number;
  discountedPrice?: number;
  rating?: number;
  totalReviews?: number;
  isNew?: boolean;
  bookingModes: BookingMode[];
  hourlyRate?: number;
  minHours?: number;
  maxHours?: number;
  hourStep?: number;
}

export interface RawCustomerServiceCard {
  id?: number | string;
  serviceId?: number | string;
  name?: string;
  serviceName?: string;
  description?: string;
  imageUrl?: string;
  price?: number | string;
  discountedPrice?: number | string;
  rating?: number | string;
  totalReviews?: number | string;
  isNew?: boolean;
  bookingModes?: string[];
  supportedBookingModes?: string[];
  hourlyRate?: number | string;
  ratePerHour?: number | string;
  pricePerHour?: number | string;
  minHours?: number | string;
  minimumHours?: number | string;
  maxHours?: number | string;
  maximumHours?: number | string;
  hourStep?: number | string;
}

export function normalizeCustomerService(raw: RawCustomerServiceCard): CustomerServiceCard {
  const bookingModes = normalizeBookingModes(raw.bookingModes ?? raw.supportedBookingModes);
  const serviceId = toNumber(raw.serviceId ?? raw.id) ?? 0;
  const serviceName = raw.serviceName ?? raw.name ?? "";

  return {
    id: toNumber(raw.id),
    name: raw.name,
    serviceId,
    serviceName,
    description: raw.description,
    imageUrl: raw.imageUrl,
    price: toNumber(raw.price),
    discountedPrice: toNumber(raw.discountedPrice),
    rating: toNumber(raw.rating),
    totalReviews: toNumber(raw.totalReviews),
    isNew: raw.isNew,
    bookingModes,
    hourlyRate: toNumber(raw.hourlyRate ?? raw.ratePerHour ?? raw.pricePerHour),
    minHours: toNumber(raw.minHours ?? raw.minimumHours),
    maxHours: toNumber(raw.maxHours ?? raw.maximumHours),
    hourStep: toNumber(raw.hourStep),
  };
}

export function normalizeCustomerServices(payload: unknown): CustomerServiceCard[] {
  const data = Array.isArray(payload)
    ? payload
    : Array.isArray((payload as { data?: unknown })?.data)
      ? (payload as { data: unknown[] }).data
      : [];

  return data.map((item) => normalizeCustomerService(item as RawCustomerServiceCard));
}

function normalizeBookingModes(value?: string[]): BookingMode[] {
  const modes = (value ?? [])
    .map((mode) => String(mode).trim().toUpperCase())
    .filter((mode): mode is BookingMode => mode === "FIXED" || mode === "HOURLY");
  return modes.length > 0 ? Array.from(new Set(modes)) : ["FIXED"];
}

function toNumber(value: unknown): number | undefined {
  if (value === null || value === undefined || value === "") {
    return undefined;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
}
