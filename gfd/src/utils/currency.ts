/**
 * Format amount as Indonesian Rupiah (IDR)
 * @param amount - The amount to format
 * @returns Formatted currency string
 */
export function formatIDR(amount: number): string {
  // Format with thousands separator and 2 decimal places
  return new Intl.NumberFormat('id-ID', {
    style: 'currency',
    currency: 'IDR',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(amount)
}

/**
 * Format amount as plain Rupiah without symbol (for internal calculations)
 * @param amount - The amount to format
 * @returns Formatted number string
 */
export function formatRupiah(amount: number): string {
  return amount.toLocaleString('id-ID', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  })
}

