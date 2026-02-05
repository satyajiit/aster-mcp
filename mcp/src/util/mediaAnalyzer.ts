/**
 * AI-powered media search result analyzer.
 * Provides human-readable insights and recommendations.
 */

interface MediaSearchResult {
  matched: number;
  files: Array<{
    path: string;
    name: string;
    size: number;
    dateTaken?: string;
    location?: {
      city?: string;
      country?: string;
    };
  }>;
}

interface MediaInsights {
  summary: string;
  locations: Map<string, number>;
  dateRange: { earliest?: string; latest?: string };
  totalSize: number;
  suggestions: string[];
}

/**
 * Analyze search results and generate insights.
 */
export function analyzeMediaResults(results: MediaSearchResult, query: string): MediaInsights {
  const insights: MediaInsights = {
    summary: '',
    locations: new Map(),
    dateRange: {},
    totalSize: 0,
    suggestions: [],
  };

  if (results.matched === 0) {
    insights.summary = `No media found matching "${query}".`;
    insights.suggestions.push('Try broadening your search criteria');
    insights.suggestions.push('Check if the date range is correct');
    return insights;
  }

  // Calculate total size
  insights.totalSize = results.files.reduce((sum, file) => sum + file.size, 0);

  // Extract locations
  results.files.forEach(file => {
    if (file.location?.city) {
      const count = insights.locations.get(file.location.city) || 0;
      insights.locations.set(file.location.city, count + 1);
    }
  });

  // Find date range
  const dates = results.files
    .map(f => f.dateTaken)
    .filter(d => d != null)
    .sort();

  if (dates.length > 0) {
    insights.dateRange.earliest = dates[0];
    insights.dateRange.latest = dates[dates.length - 1];
  }

  // Generate summary
  insights.summary = generateSummary(results, insights);

  // Generate suggestions
  insights.suggestions = generateSuggestions(results, insights);

  return insights;
}

function generateSummary(results: MediaSearchResult, insights: MediaInsights): string {
  const count = results.matched;
  const sizeMB = insights.totalSize.toFixed(1);

  let summary = `Found ${count} file${count !== 1 ? 's' : ''} (${sizeMB} MB)`;

  if (insights.locations.size > 0) {
    const topLocations = Array.from(insights.locations.entries())
      .sort((a, b) => b[1] - a[1])
      .slice(0, 3)
      .map(([city, count]) => `${city} (${count})`)
      .join(', ');

    summary += `\nTop locations: ${topLocations}`;
  }

  if (insights.dateRange.earliest && insights.dateRange.latest) {
    summary += `\nDate range: ${insights.dateRange.earliest} to ${insights.dateRange.latest}`;
  }

  return summary;
}

function generateSuggestions(results: MediaSearchResult, insights: MediaInsights): string[] {
  const suggestions: string[] = [];

  // Size-based suggestions
  if (insights.totalSize > 1000) {
    suggestions.push('Consider backing up these files to cloud storage');
  }

  if (insights.totalSize > 500 && results.matched > 50) {
    suggestions.push('You could free up significant space by archiving these files');
  }

  // Location-based suggestions
  if (insights.locations.size > 3) {
    suggestions.push('Photos span multiple locations - would you like to organize by location?');
  }

  // Count-based suggestions
  if (results.matched > 100) {
    suggestions.push('Large result set - consider filtering by date or location');
  }

  return suggestions;
}
