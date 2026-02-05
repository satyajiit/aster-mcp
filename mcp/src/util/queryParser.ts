/**
 * Natural language query parser for media search.
 * Converts user queries like "photos from last year" into structured filters.
 */

interface ParsedQuery {
  dateFrom?: string;
  dateTo?: string;
  location?: {
    city?: string;
    country?: string;
    latitude?: number;
    longitude?: number;
    radiusKm?: number;
  };
  fileTypes?: string[];
  keywords?: string[];
}

/**
 * Parse natural language time expressions.
 */
export function parseTimeExpression(query: string, currentDate: Date = new Date()): { dateFrom?: string; dateTo?: string } | null {
  const queryLower = query.toLowerCase();

  // "last year same month" -> previous year, same month
  if (queryLower.includes('last year same month') || queryLower.includes('same month last year')) {
    const lastYear = new Date(currentDate);
    lastYear.setFullYear(lastYear.getFullYear() - 1);

    const year = lastYear.getFullYear();
    const month = lastYear.getMonth() + 1;
    const lastDay = new Date(year, month, 0).getDate();

    return {
      dateFrom: `${year}-${String(month).padStart(2, '0')}-01`,
      dateTo: `${year}-${String(month).padStart(2, '0')}-${lastDay}`,
    };
  }

  // "last month" -> previous calendar month
  if (queryLower.includes('last month')) {
    const lastMonth = new Date(currentDate);
    lastMonth.setMonth(lastMonth.getMonth() - 1);

    const year = lastMonth.getFullYear();
    const month = lastMonth.getMonth() + 1;
    const lastDay = new Date(year, month, 0).getDate();

    return {
      dateFrom: `${year}-${String(month).padStart(2, '0')}-01`,
      dateTo: `${year}-${String(month).padStart(2, '0')}-${lastDay}`,
    };
  }

  // "last week" -> 7 days ago to today
  if (queryLower.includes('last week')) {
    const weekAgo = new Date(currentDate);
    weekAgo.setDate(weekAgo.getDate() - 7);

    return {
      dateFrom: formatDate(weekAgo),
      dateTo: formatDate(currentDate),
    };
  }

  // "last 30 days" or "last month"
  const daysMatch = queryLower.match(/last (\d+) days?/);
  if (daysMatch) {
    const days = parseInt(daysMatch[1]);
    const startDate = new Date(currentDate);
    startDate.setDate(startDate.getDate() - days);

    return {
      dateFrom: formatDate(startDate),
      dateTo: formatDate(currentDate),
    };
  }

  // "this year" -> January 1 to today
  if (queryLower.includes('this year')) {
    return {
      dateFrom: `${currentDate.getFullYear()}-01-01`,
      dateTo: formatDate(currentDate),
    };
  }

  // "last year" -> entire previous year
  if (queryLower.includes('last year')) {
    const lastYear = currentDate.getFullYear() - 1;
    return {
      dateFrom: `${lastYear}-01-01`,
      dateTo: `${lastYear}-12-31`,
    };
  }

  // "2025" -> entire year
  const yearMatch = queryLower.match(/\b(20\d{2})\b/);
  if (yearMatch) {
    const year = yearMatch[1];
    return {
      dateFrom: `${year}-01-01`,
      dateTo: `${year}-12-31`,
    };
  }

  // Month names
  const months = ['january', 'february', 'march', 'april', 'may', 'june',
                  'july', 'august', 'september', 'october', 'november', 'december'];

  for (let i = 0; i < months.length; i++) {
    if (queryLower.includes(months[i])) {
      const month = i + 1;
      const currentMonth = currentDate.getMonth() + 1;
      // If the month is in the future, assume user means previous year
      // e.g., "April" in February 2026 -> April 2025
      const year = month > currentMonth
        ? currentDate.getFullYear() - 1
        : currentDate.getFullYear();
      const lastDay = new Date(year, month, 0).getDate();

      return {
        dateFrom: `${year}-${String(month).padStart(2, '0')}-01`,
        dateTo: `${year}-${String(month).padStart(2, '0')}-${lastDay}`,
      };
    }
  }

  return null;
}

/**
 * Parse location mentions from query.
 */
export function parseLocation(query: string): { city?: string; country?: string } | null {
  // Exclude month names and time-related words from being treated as locations
  const excludedWords = new Set([
    'january', 'february', 'march', 'april', 'may', 'june',
    'july', 'august', 'september', 'october', 'november', 'december',
    'last', 'this', 'next', 'year', 'month', 'week', 'day', 'today', 'yesterday'
  ]);

  // Common patterns: "in [city]", "at [city]", "from [city]", "taken in [city]"
  const locationPatterns = [
    /(?:in|at|from|near|around)\s+([A-Z][a-zA-Z\s]+?)(?:\s|$|,)/,
    /taken\s+(?:in|at)\s+([A-Z][a-zA-Z\s]+?)(?:\s|$|,)/,
  ];

  for (const pattern of locationPatterns) {
    const match = query.match(pattern);
    if (match) {
      const location = match[1].trim();

      // Skip if the matched location is a time-related word
      if (excludedWords.has(location.toLowerCase())) {
        continue;
      }

      // Simple heuristic: if it's one word, treat as city, if multiple might be city + country
      const parts = location.split(/\s+/);
      if (parts.length === 1) {
        return { city: location };
      } else {
        // Last word might be country
        return {
          city: parts.slice(0, -1).join(' '),
          country: parts[parts.length - 1]
        };
      }
    }
  }

  return null;
}

/**
 * Parse file type mentions.
 */
export function parseFileTypes(query: string): string[] | null {
  const queryLower = query.toLowerCase();

  if (queryLower.includes('photo') || queryLower.includes('picture') || queryLower.includes('image')) {
    return ['jpg', 'jpeg', 'png', 'heic', 'webp'];
  }

  if (queryLower.includes('video') || queryLower.includes('movie')) {
    return ['mp4', 'mov', 'avi', 'mkv', '3gp'];
  }

  if (queryLower.includes('screenshot')) {
    return ['png']; // Screenshots are usually PNG on Android
  }

  if (queryLower.includes('selfie')) {
    // Note: This would need camera info to detect front camera
    return ['jpg', 'jpeg', 'png', 'heic'];
  }

  return null;
}

/**
 * Format date as ISO string (YYYY-MM-DD).
 */
function formatDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/**
 * Parse complete natural language query into structured filters.
 */
export function parseNaturalLanguageQuery(query: string): ParsedQuery {
  const result: ParsedQuery = {};

  // Parse time expression
  const timeFilter = parseTimeExpression(query);
  if (timeFilter) {
    result.dateFrom = timeFilter.dateFrom;
    result.dateTo = timeFilter.dateTo;
  }

  // Parse location
  const locationFilter = parseLocation(query);
  if (locationFilter) {
    result.location = locationFilter;
  }

  // Parse file types
  const fileTypes = parseFileTypes(query);
  if (fileTypes) {
    result.fileTypes = fileTypes;
  }

  // Extract keywords (words that aren't common stop words)
  const stopWords = new Set(['the', 'a', 'an', 'in', 'at', 'from', 'to', 'of', 'for', 'with', 'on', 'by', 'my', 'me', 'i']);
  const words = query.toLowerCase().split(/\s+/).filter(word =>
    word.length > 2 && !stopWords.has(word) && !/^\d+$/.test(word)
  );
  if (words.length > 0) {
    result.keywords = words;
  }

  return result;
}
