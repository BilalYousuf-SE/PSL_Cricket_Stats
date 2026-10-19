// PUT bodies include null for cleared fields so optional values can be removed.
export function serializeForm(fields, values) {
  return Object.fromEntries(fields.map((field) => {
    const value = values[field.n];
    if (field.t === 'bool') return [field.n, Boolean(value)];
    if (value === '' || value == null) return [field.n, null];
    const numeric = field.t === 'number' || field.numeric || (field.t === 'select' && field.optionsFrom);
    return [field.n, numeric ? Number(value) : value];
  }));
}
