package com.ivy.home

import androidx.annotation.DrawableRes
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ivy.base.model.TransactionType
import com.ivy.design.api.LocalTimeConverter
import com.ivy.design.api.LocalTimeFormatter
import com.ivy.design.api.LocalTimeProvider
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.design.utils.thenIf
import com.ivy.legacy.data.model.PeriodType
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.ui.component.transaction.TransactionsDividerLine
import com.ivy.legacy.utils.clickableNoIndication
import com.ivy.legacy.utils.drawColoredShadow
import com.ivy.legacy.utils.format
import com.ivy.legacy.utils.horizontalSwipeListener
import com.ivy.legacy.utils.isNotNullOrBlank
import com.ivy.legacy.utils.rememberInteractionSource
import com.ivy.legacy.utils.rememberSwipeListenerState
import com.ivy.legacy.utils.springBounce
import com.ivy.legacy.utils.verticalSwipeListener
import com.ivy.navigation.PieChartStatisticScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.Gradient
import com.ivy.wallet.ui.theme.GradientGreen
import com.ivy.wallet.ui.theme.Gray
import com.ivy.wallet.ui.theme.Green
import com.ivy.wallet.ui.theme.White
import com.ivy.wallet.ui.theme.components.BalanceRow
import com.ivy.wallet.ui.theme.components.BalanceRowMini
import com.ivy.wallet.ui.theme.components.IvyIcon
import com.ivy.wallet.ui.theme.components.IvyOutlinedButton
import com.ivy.wallet.ui.theme.wallet.AmountCurrencyB1
import kotlin.math.absoluteValue

@ExperimentalAnimationApi
@Composable
internal fun HomeHeader(
    expanded: Boolean,
    name: String,
    period: TimePeriod,
    currency: String,
    balance: Double,
    onShowMonthModal: () -> Unit,
    onBalanceClick: () -> Unit,
    onSelectNextMonth: () -> Unit,
    hideBalance: Boolean,
    onHiddenBalanceClick: () -> Unit,
    onSelectPreviousMonth: () -> Unit,
    onSelectPeriod: (TimePeriod) -> Unit = {},
) {
    Column {
        val percentExpanded by animateFloatAsState(
            targetValue = if (expanded) 1f else 0f,
            animationSpec = springBounce(
                stiffness = Spring.StiffnessLow
            ),
            label = "Home Header Expand Collapse"
        )

        Spacer(Modifier.height(20.dp))

        HeaderStickyRow(
            percentExpanded = percentExpanded,
            name = name,
            period = period,
            currency = currency,
            balance = balance,
            hideBalance = hideBalance,

            onShowMonthModal = onShowMonthModal,
            onBalanceClick = onBalanceClick,
            onHiddenBalanceClick = onHiddenBalanceClick,
            onSelectNextMonth = onSelectNextMonth,
            onSelectPreviousMonth = onSelectPreviousMonth,
            onSelectPeriod = onSelectPeriod,
        )

        Spacer(Modifier.height(16.dp))

        if (percentExpanded < 0.5f) {
            TransactionsDividerLine(
                modifier = Modifier.alpha(1f - percentExpanded),
                paddingHorizontal = 0.dp
            )
        }
    }
}

@Composable
private fun HeaderStickyRow(
    percentExpanded: Float,
    name: String,
    period: TimePeriod,
    currency: String,
    balance: Double,
    onShowMonthModal: () -> Unit,
    onBalanceClick: () -> Unit,
    onSelectNextMonth: () -> Unit,
    hideBalance: Boolean,
    onHiddenBalanceClick: () -> Unit,
    onSelectPreviousMonth: () -> Unit,
    onSelectPeriod: (TimePeriod) -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                modifier = Modifier
                    .alpha(percentExpanded)
                    .testTag("home_greeting_text"),
                text = if (name.isNotNullOrBlank()) {
                    stringResource(
                        R.string.hi_name,
                        name,
                    )
                } else {
                    stringResource(R.string.hi)
                },
                style = UI.typo.b1.style(
                    fontWeight = FontWeight.ExtraBold,
                    color = UI.colors.pureInverse,
                ),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )

            // Balance mini row
            if (percentExpanded < 1f) {
                BalanceRowMini(
                    modifier = Modifier
                        .alpha(alpha = 1f - percentExpanded)
                        .clickableNoIndication(rememberInteractionSource()) {
                            if (hideBalance) {
                                onHiddenBalanceClick()
                            } else {
                                onBalanceClick()
                            }
                        },
                    currency = currency,
                    balance = balance,
                    shortenBigNumbers = true,
                    hiddenMode = hideBalance,
                    doubleRowDisplay = true,
                )
            }
        }

        var dropdownExpanded by remember { mutableStateOf(false) }
        val startDayOfMonth = ivyWalletCtx().startDayOfMonth

        Box {
            IvyOutlinedButton(
                modifier = Modifier.horizontalSwipeListener(
                    sensitivity = 75,
                    state = rememberSwipeListenerState(),
                    onSwipeLeft = {
                        onSelectNextMonth()
                    },
                    onSwipeRight = {
                        onSelectPreviousMonth()
                    },
                ),
                iconStart = R.drawable.ic_calendar,
                text = period.toDisplayShort(
                    startDateOfMonth = startDayOfMonth,
                    timeConverter = LocalTimeConverter.current,
                    timeProvider = LocalTimeProvider.current,
                    timeFormatter = LocalTimeFormatter.current,
                ),
                minWidth = 130.dp,
            ) {
                dropdownExpanded = true
            }

            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false },
                modifier = Modifier.background(UI.colors.pure)
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Today (Day)",
                            style = UI.typo.b2.style(
                                color = UI.colors.pureInverse,
                                fontWeight = if (period.periodType == PeriodType.DAY) FontWeight.ExtraBold else FontWeight.Normal
                            )
                        )
                    },
                    onClick = {
                        dropdownExpanded = false
                        onSelectPeriod(TimePeriod.today())
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "This Week (Week)",
                            style = UI.typo.b2.style(
                                color = UI.colors.pureInverse,
                                fontWeight = if (period.periodType == PeriodType.WEEK) FontWeight.ExtraBold else FontWeight.Normal
                            )
                        )
                    },
                    onClick = {
                        dropdownExpanded = false
                        onSelectPeriod(TimePeriod.currentWeek())
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "This Month (Month)",
                            style = UI.typo.b2.style(
                                color = UI.colors.pureInverse,
                                fontWeight = if (period.periodType == PeriodType.MONTH) FontWeight.ExtraBold else FontWeight.Normal
                            )
                        )
                    },
                    onClick = {
                        dropdownExpanded = false
                        onSelectPeriod(TimePeriod.currentMonth(startDayOfMonth))
                    }
                )
                HorizontalDivider(color = UI.colors.medium)
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "More options...",
                            style = UI.typo.b2.style(
                                color = UI.colors.gray,
                                fontWeight = FontWeight.Normal
                            )
                        )
                    },
                    onClick = {
                        dropdownExpanded = false
                        onShowMonthModal()
                    }
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Spacer(Modifier.width(40.dp)) // settings menu button spacer
    }
}

@Composable
fun TotalBalanceCard(
    currency: String,
    balance: Double,
    hideBalance: Boolean,
    onBalanceClick: () -> Unit,
    onHiddenBalanceClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(UI.shapes.r4)
            .background(UI.colors.medium.copy(alpha = 0.35f))
            .clickableNoIndication(rememberInteractionSource()) {
                if (hideBalance) {
                    onHiddenBalanceClick()
                } else {
                    onBalanceClick()
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .clip(UI.shapes.rFull)
                    .background(UI.colors.primary.copy(alpha = 0.15f))
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                IvyIcon(
                    icon = R.drawable.ic_wallet,
                    tint = UI.colors.primary,
                )
            }

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    text = "Total money",
                    style = UI.typo.c.style(
                        color = UI.colors.gray,
                        fontWeight = FontWeight.Bold,
                    )
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = if (hideBalance) {
                        "••••••"
                    } else {
                        "${balance.format(currency)} $currency"
                    },
                    style = UI.typo.b1.style(
                        color = UI.colors.pureInverse,
                        fontWeight = FontWeight.ExtraBold,
                    )
                )
            }
        }

        IvyIcon(
            icon = R.drawable.ic_arrow_right,
            tint = UI.colors.gray,
        )
    }
}

@ExperimentalAnimationApi
@Composable
fun CashFlowInfo(
    currency: String,
    balance: Double,
    monthlyIncome: Double,
    monthlyExpenses: Double,
    hideBalance: Boolean,
    hideIncome: Boolean,
    onHiddenIncomeClick: () -> Unit,
    onOpenMoreMenu: () -> Unit,
    onBalanceClick: () -> Unit,
    percentExpanded: Float,
    onHiddenBalanceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalSwipeListener(
                sensitivity = Constants.SWIPE_DOWN_THRESHOLD_OPEN_MORE_MENU,
                state = rememberSwipeListenerState(),
                onSwipeDown = {
                    onOpenMoreMenu()
                },
            ),
    ) {
        BalanceRow(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .clickableNoIndication(rememberInteractionSource()) {
                    if (hideBalance) {
                        onHiddenBalanceClick()
                    } else {
                        onBalanceClick()
                    }
                }
                .testTag("home_balance"),
            currency = currency,
            balance = balance,
            shortenBigNumbers = true,
            hiddenMode = hideBalance
        )

        Spacer(modifier = Modifier.height(16.dp))

        TotalBalanceCard(
            currency = currency,
            balance = balance,
            hideBalance = hideBalance,
            onBalanceClick = onBalanceClick,
            onHiddenBalanceClick = onHiddenBalanceClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        IncomeExpenses(
            percentExpanded = percentExpanded,
            currency = currency,
            monthlyIncome = monthlyIncome,
            monthlyExpenses = monthlyExpenses,
            hideIncome = hideIncome,
            onHiddenIncomeClick = onHiddenIncomeClick
        )

        val cashflow = monthlyIncome - monthlyExpenses
        if (cashflow != 0.0 && !hideBalance) {
            Spacer(Modifier.height(12.dp))

            Text(
                modifier = Modifier.padding(
                    start = 24.dp,
                ),
                text = stringResource(
                    R.string.cashflow,
                    (if (cashflow > 0) "+" else ""),
                    cashflow.format(currency),
                    currency,
                ),
                style = UI.typo.nB2.style(
                    color = if (cashflow < 0) Gray else Green,
                ),
            )

            Spacer(Modifier.height(4.dp))
        } else {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun IncomeExpenses(
    percentExpanded: Float,
    currency: String,
    monthlyIncome: Double,
    monthlyExpenses: Double,
    hideIncome: Boolean,
    onHiddenIncomeClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(16.dp))

        val nav = navigation()

        HeaderCard(
            percentVisible = percentExpanded,
            icon = R.drawable.ic_income,
            backgroundGradient = GradientGreen,
            textColor = White,
            label = stringResource(R.string.income),
            currency = currency,
            amount = monthlyIncome,
            testTag = "home_card_income"
        ) {
            if (hideIncome) {
                onHiddenIncomeClick()
            } else {
                nav.navigateTo(
                    PieChartStatisticScreen(
                        type = TransactionType.INCOME,
                    ),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        HeaderCard(
            percentVisible = percentExpanded,
            icon = R.drawable.ic_expense,
            backgroundGradient = Gradient(UI.colors.pureInverse, UI.colors.gray),
            textColor = UI.colors.pure,
            label = stringResource(R.string.expenses),
            currency = currency,
            amount = monthlyExpenses.absoluteValue,
            testTag = "home_card_expense",
        ) {
            nav.navigateTo(
                PieChartStatisticScreen(
                    type = TransactionType.EXPENSE,
                ),
            )
        }

        Spacer(Modifier.width(16.dp))
    }
}

@Composable
private fun RowScope.HeaderCard(
    @DrawableRes icon: Int,
    backgroundGradient: Gradient,
    percentVisible: Float,
    textColor: Color,
    label: String,
    currency: String,
    amount: Double,
    testTag: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .thenIf(percentVisible == 1f) {
                drawColoredShadow(backgroundGradient.startColor)
            }
            .clip(UI.shapes.r4)
            .background(backgroundGradient.asHorizontalBrush())
            .testTag(testTag)
            .clickable(
                onClick = onClick,
            ),
    ) {
        Spacer(Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(16.dp))

            IvyIcon(
                icon = icon,
                tint = textColor,
            )

            Spacer(Modifier.width(4.dp))

            Text(
                text = label,
                style = UI.typo.c.style(
                    color = textColor,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )
        }

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(20.dp))

            AmountCurrencyB1(
                amount = amount,
                currency = currency,
                textColor = textColor,
                shortenBigNumbers = true,
            )

            Spacer(Modifier.width(4.dp))
        }

        Spacer(Modifier.height(20.dp))
    }
}
