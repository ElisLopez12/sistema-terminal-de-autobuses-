export function extractError(err) {
  const data = err.response?.data
  if (data?.fieldErrors?.length) {
    return data.fieldErrors.map((f) => `${f.field}: ${f.message}`).join(', ')
  }
  return data?.message ?? data?.error ?? err.message
}
