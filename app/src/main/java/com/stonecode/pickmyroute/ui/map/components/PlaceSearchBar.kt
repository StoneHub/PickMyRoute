package com.stonecode.pickmyroute.ui.map.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stonecode.pickmyroute.domain.model.PlacePrediction
import com.stonecode.pickmyroute.ui.theme.PickMyRouteTheme

/**
 * Google Maps-style search bar with autocomplete suggestions
 * Supports both place name search and direct address entry
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceSearchBar(
    searchQuery: String,
    predictions: List<PlacePrediction>,
    isSearching: Boolean,
    isExpanded: Boolean,
    onQueryChange: (String) -> Unit,
    onResultSelected: (String) -> Unit,
    onExpandChange: (Boolean) -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
    ) {
        // Search input field
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isExpanded) 8.dp else 4.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    placeholder = {
                        Text(
                            text = if (isExpanded) "Search for places..." else "🔍 Search or tap map",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                )

                // Loading indicator or clear button
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onClearSearch()
                            focusManager.clearFocus()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Autocomplete suggestions dropdown
        AnimatedVisibility(
            visible = isExpanded && predictions.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    items(predictions) { prediction ->
                        SearchResultItem(
                            prediction = prediction,
                            onClick = {
                                onResultSelected(prediction.placeId)
                                onExpandChange(false)
                                focusManager.clearFocus()
                            }
                        )
                        if (prediction != predictions.last()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Auto-focus when expanded
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            focusRequester.requestFocus()
        } else {
            focusManager.clearFocus()
        }
    }
}

/**
 * Individual search result item in the dropdown
 */
@Composable
private fun SearchResultItem(
    prediction: PlacePrediction,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Location icon
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(20.dp)
                .padding(end = 4.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Text content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Main text (place name)
            Text(
                text = prediction.mainText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Secondary text (address)
            if (prediction.secondaryText.isNotEmpty()) {
                Text(
                    text = prediction.secondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Preview functions
@Preview(name = "Search Bar - Empty", showBackground = true)
@Composable
private fun PlaceSearchBarPreviewEmpty() {
    PickMyRouteTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            PlaceSearchBar(
                searchQuery = "",
                predictions = emptyList(),
                isSearching = false,
                isExpanded = false,
                onQueryChange = {},
                onResultSelected = {},
                onExpandChange = {},
                onClearSearch = {}
            )
        }
    }
}

@Preview(name = "Search Bar - Typing", showBackground = true)
@Composable
private fun PlaceSearchBarPreviewTyping() {
    PickMyRouteTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            PlaceSearchBar(
                searchQuery = "Starb",
                predictions = emptyList(),
                isSearching = true,
                isExpanded = true,
                onQueryChange = {},
                onResultSelected = {},
                onExpandChange = {},
                onClearSearch = {}
            )
        }
    }
}

@Preview(name = "Search Bar - With Results", showBackground = true)
@Composable
private fun PlaceSearchBarPreviewWithResults() {
    PickMyRouteTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            PlaceSearchBar(
                searchQuery = "Starbucks",
                predictions = listOf(
                    PlacePrediction(
                        placeId = "1",
                        mainText = "Starbucks",
                        secondaryText = "123 Main St, San Francisco, CA",
                        fullText = "Starbucks, 123 Main St, San Francisco, CA"
                    ),
                    PlacePrediction(
                        placeId = "2",
                        mainText = "Starbucks Coffee",
                        secondaryText = "456 Market St, San Francisco, CA",
                        fullText = "Starbucks Coffee, 456 Market St, San Francisco, CA"
                    ),
                    PlacePrediction(
                        placeId = "3",
                        mainText = "Starbucks Reserve",
                        secondaryText = "789 Mission St, San Francisco, CA",
                        fullText = "Starbucks Reserve, 789 Mission St, San Francisco, CA"
                    )
                ),
                isSearching = false,
                isExpanded = true,
                onQueryChange = {},
                onResultSelected = {},
                onExpandChange = {},
                onClearSearch = {}
            )
        }
    }
}

@Preview(name = "Search Bar - Dark Mode", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlaceSearchBarPreviewDark() {
    PickMyRouteTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            PlaceSearchBar(
                searchQuery = "Golden Gate Bridge",
                predictions = listOf(
                    PlacePrediction(
                        placeId = "1",
                        mainText = "Golden Gate Bridge",
                        secondaryText = "San Francisco, CA 94129",
                        fullText = "Golden Gate Bridge, San Francisco, CA 94129"
                    ),
                    PlacePrediction(
                        placeId = "2",
                        mainText = "Golden Gate Park",
                        secondaryText = "San Francisco, CA",
                        fullText = "Golden Gate Park, San Francisco, CA"
                    )
                ),
                isSearching = false,
                isExpanded = true,
                onQueryChange = {},
                onResultSelected = {},
                onExpandChange = {},
                onClearSearch = {}
            )
        }
    }
}
