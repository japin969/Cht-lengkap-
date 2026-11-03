package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Country
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramCardDark
import com.example.ui.theme.TelegramCardLight
import com.example.ui.theme.TelegramSeparatorDark
import com.example.ui.theme.TelegramSeparatorLight
import com.example.ui.theme.TelegramSubtext
import com.example.ui.theme.TelegramTheme

@Composable
fun CountryPickerDialog(
    countries: List<Country>,
    selectedCountry: Country?,
    onCountrySelected: (Country) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val containerColor = if (isDark) TelegramCardDark else TelegramCardLight
    val textColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val dividerColor = if (isDark) TelegramSeparatorDark else TelegramSeparatorLight

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = containerColor,
        title = {
            Text(
                text = stringResource(id = R.string.auth_country_dialog_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .testTag("country_picker_list")
            ) {
                items(
                    items = countries,
                    key = { it.code }
                ) { country ->
                    val isSelected = selectedCountry?.code == country.code
                    CountryRowItem(
                        country = country,
                        isSelected = isSelected,
                        textColor = textColor,
                        onClick = {
                            onCountrySelected(country)
                            onDismiss()
                        }
                    )
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = dividerColor
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("country_picker_cancel_button")
            ) {
                Text(
                    text = stringResource(id = R.string.auth_cancel_button),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TelegramBlue
                )
            }
        }
    )
}

@Composable
fun CountryRowItem(
    country: Country,
    isSelected: Boolean,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp)
            .testTag("country_item_${country.code}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(TelegramBlue.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                // GANTI SVG TELEGRAM
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = stringResource(id = R.string.auth_country_flag_cd),
                    tint = TelegramBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = country.name,
                    fontSize = 16.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = textColor
                )
                Text(
                    text = country.code,
                    fontSize = 12.sp,
                    color = TelegramSubtext
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = country.dialCode,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TelegramBlue
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = country.name,
                    tint = TelegramBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Preview(name = "Country Picker Light", showBackground = true)
@Composable
private fun CountryPickerDialogLightPreview() {
    val sampleCountries = listOf(
        Country(name = "Indonesia", code = "ID", dialCode = "+62"),
        Country(name = "Amerika Serikat", code = "US", dialCode = "+1"),
        Country(name = "Inggris", code = "GB", dialCode = "+44"),
        Country(name = "India", code = "IN", dialCode = "+91"),
        Country(name = "Jepang", code = "JP", dialCode = "+81")
    )
    TelegramTheme(darkTheme = false) {
        Surface(color = Color(0xFFE6EBEE)) {
            Column(modifier = Modifier.padding(16.dp)) {
                sampleCountries.forEach { country ->
                    CountryRowItem(
                        country = country,
                        isSelected = country.code == "ID",
                        textColor = Color(0xFF000000),
                        onClick = {}
                    )
                }
            }
        }
    }
}

@Preview(name = "Country Picker Dark", showBackground = true)
@Composable
private fun CountryPickerDialogDarkPreview() {
    val sampleCountries = listOf(
        Country(name = "Indonesia", code = "ID", dialCode = "+62"),
        Country(name = "Amerika Serikat", code = "US", dialCode = "+1"),
        Country(name = "Inggris", code = "GB", dialCode = "+44"),
        Country(name = "India", code = "IN", dialCode = "+91"),
        Country(name = "Jepang", code = "JP", dialCode = "+81")
    )
    TelegramTheme(darkTheme = true) {
        Surface(color = TelegramCardDark) {
            Column(modifier = Modifier.padding(16.dp)) {
                sampleCountries.forEach { country ->
                    CountryRowItem(
                        country = country,
                        isSelected = country.code == "JP",
                        textColor = Color(0xFFFFFFFF),
                        onClick = {}
                    )
                }
            }
        }
    }
}
