package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.ActivityLedgerSnapshot
import com.kedevjimmy.pesatrack.domain.model.CheckedMoney
import java.util.Calendar

private val aqua = Color(0xFF52E5D3)
private val violet = Color(0xFFC78CFA)

internal fun monthDaysRemaining(selected: Long, now: Long): Int? {
    val a = Calendar.getInstance().apply { timeInMillis = selected }
    val b = Calendar.getInstance().apply { timeInMillis = now }
    if(a.get(Calendar.YEAR)!=b.get(Calendar.YEAR) ||
       a.get(Calendar.MONTH)!=b.get(Calendar.MONTH)) return null
    return b.getActualMaximum(Calendar.DAY_OF_MONTH) - b.get(Calendar.DAY_OF_MONTH)
}
internal fun periodPercentChange(current: Long, previous: Long): Int? {
    if(previous<=0L || current<0L) return null
    return runCatching {
        java.math.BigDecimal.valueOf(current).subtract(java.math.BigDecimal.valueOf(previous))
            .multiply(java.math.BigDecimal.valueOf(100))
            .divide(java.math.BigDecimal.valueOf(previous),0,java.math.RoundingMode.HALF_UP)
            .intValueExact()
    }.getOrNull()
}

@Composable
internal fun ActivityAttentionHero(count:Int,onReview:()->Unit) {
    if(count<=0)return
    Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),
        color=MaterialTheme.colorScheme.surface,
        border=BorderStroke(1.dp,violet.copy(alpha=.7f))) {
        Box {
            Canvas(Modifier.matchParentSize()) {
                drawRect(Brush.horizontalGradient(listOf(
                    violet.copy(alpha=.19f),Color.Transparent,aqua.copy(alpha=.11f))))
            }
            Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                Surface(shape=CircleShape,color=violet.copy(alpha=.20f)) {
                    Icon(Icons.Rounded.PriorityHigh,null,Modifier.padding(9.dp).size(22.dp),tint=violet)
                }
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                    Text("WHAT NEEDS MY ATTENTION?",style=MaterialTheme.typography.labelSmall,color=violet)
                    Text(count.toString()+" "+(if(count==1)"item needs review" else "items need review"),
                        style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold)
                    Text("Review uncertain categories, duplicates and account links.",
                        style=MaterialTheme.typography.labelSmall,maxLines=2,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick=onReview,shape=RoundedCornerShape(12.dp),
                    contentPadding=PaddingValues(horizontal=8.dp,vertical=7.dp),
                    colors=ButtonDefaults.buttonColors(containerColor=violet,contentColor=Color(0xFF19102B))) {
                    Text("Review now",style=MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
internal fun ActivityPeriodSelectors(
    monthLabel:String,cycleLabel:String,salaryMode:Boolean,
    onMonth:()->Unit,onCycle:()->Unit
) {
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        PeriodTile(monthLabel,Icons.Rounded.CalendarMonth,!salaryMode,onMonth,Modifier.weight(1f))
        PeriodTile(if(salaryMode)cycleLabel else "Salary cycle",
            Icons.Rounded.Autorenew,salaryMode,onCycle,Modifier.weight(1f))
    }
}
@Composable private fun PeriodTile(
    title:String,icon:androidx.compose.ui.graphics.vector.ImageVector,
    selected:Boolean,onClick:()->Unit,modifier:Modifier
) {
    Surface(onClick=onClick,modifier=modifier.heightIn(min=48.dp),
        shape=RoundedCornerShape(15.dp),color=MaterialTheme.colorScheme.surface,
        border=BorderStroke(1.dp,if(selected)aqua.copy(alpha=.5f)
            else MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(horizontal=8.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            Icon(icon,null,Modifier.size(18.dp),tint=if(selected)aqua else violet)
            Text(title,Modifier.weight(1f),style=MaterialTheme.typography.labelMedium,
                fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
            Icon(Icons.Rounded.ExpandMore,"Choose period",Modifier.size(17.dp))
        }
    }
}

@Composable
internal fun ActivityPremiumMoneyHero(
    snapshot:ActivityLedgerSnapshot?,previous:ActivityLedgerSnapshot?,
    badge:String,waiting:Boolean,onExplain:()->Unit
) {
    Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),
        color=MaterialTheme.colorScheme.surface,border=BorderStroke(1.dp,aqua.copy(alpha=.6f))) {
        Box(Modifier.fillMaxWidth().heightIn(min=171.dp)) {
            Canvas(Modifier.matchParentSize()) {
                drawRect(Brush.verticalGradient(listOf(aqua.copy(alpha=.13f),Color.Transparent)))
                val w=size.width;val h=size.height
                val path=Path().apply {
                    moveTo(0f,h*.91f)
                    cubicTo(w*.18f,h*.7f,w*.28f,h*.97f,w*.48f,h*.83f)
                    cubicTo(w*.63f,h*.67f,w*.78f,h*.94f,w,h*.78f)
                }
                drawPath(path,aqua.copy(alpha=.6f),
                    style=Stroke(width=2.dp.toPx(),cap=StrokeCap.Round))
            }
            Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
                    Surface(shape=RoundedCornerShape(14.dp),color=aqua.copy(alpha=.12f)) {
                        Text(badge,Modifier.padding(horizontal=9.dp,vertical=4.dp),
                            style=MaterialTheme.typography.labelSmall,color=aqua)
                    }
                }
                if(waiting || snapshot==null) {
                    Text("Updating this period…",Modifier.padding(top=28.dp),
                        style=MaterialTheme.typography.titleMedium)
                } else {
                    val moneyIn=snapshot.summary.incomeMinor
                    val moneyOut=snapshot.summary.spentMinor
                    val net=CheckedMoney.orNull { CheckedMoney.subtract(moneyIn,moneyOut) }
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                            Text("Money in",style=MaterialTheme.typography.labelMedium,
                                color=MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatActivityMoney(moneyIn),style=MaterialTheme.typography.titleMedium,
                                fontWeight=FontWeight.Bold,maxLines=1)
                            PercentChange(periodPercentChange(moneyIn,previous?.summary?.incomeMinor?:0))
                        }
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                            Text("Money out",style=MaterialTheme.typography.labelMedium,
                                color=MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatActivityMoney(moneyOut),style=MaterialTheme.typography.titleMedium,
                                fontWeight=FontWeight.Bold,maxLines=1)
                            PercentChange(periodPercentChange(moneyOut,previous?.summary?.spentMinor?:0))
                        }
                        Box(Modifier.size(105.dp),contentAlignment=Alignment.Center) {
                            val spendingRatio=if(moneyIn<=0L) 1f else
                                (moneyOut.toDouble()/moneyIn.toDouble()).toFloat().coerceIn(0f,1f)
                            Canvas(Modifier.fillMaxSize()) {
                                drawArc(Color(0xFF2E4254),135f,270f,false,
                                    style=Stroke(9.dp.toPx(),cap=StrokeCap.Round))
                                drawArc(aqua,135f,270f*(1f-spendingRatio),false,
                                    style=Stroke(9.dp.toPx(),cap=StrokeCap.Round))
                            }
                            Column(horizontalAlignment=Alignment.CenterHorizontally) {
                                Text(net?.let(::formatActivityMoney) ?: "Unavailable",
                                    style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.Bold,
                                    maxLines=1)
                                Text("Net flow",style=MaterialTheme.typography.labelSmall,color=aqua)
                                IconButton(onClick=onExplain,modifier=Modifier.size(26.dp)) {
                                    Icon(Icons.Rounded.Info,"Explain net flow",Modifier.size(16.dp),tint=aqua)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable private fun PercentChange(change:Int?) {
    if(change==null) {
        Text("No prior comparison",style=MaterialTheme.typography.labelSmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1)
    } else Row(horizontalArrangement=Arrangement.spacedBy(2.dp),
        verticalAlignment=Alignment.CenterVertically) {
        Icon(if(change<0)Icons.Rounded.SouthWest else Icons.Rounded.NorthEast,
            null,Modifier.size(13.dp),tint=if(change<0)violet else aqua)
        Text((if(change>0)"+" else "")+change.toString()+"%",
            style=MaterialTheme.typography.labelSmall,color=if(change<0)violet else aqua)
        Text("vs prior",style=MaterialTheme.typography.labelSmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1)
    }
}
@Composable
internal fun ActivityCompactFilters(selected:String,count:Int,onSelect:(String)->Unit) {
    Surface(shape=RoundedCornerShape(17.dp),color=MaterialTheme.colorScheme.surface,
        border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            listOf("All","Reviews","Pending","Transfers").forEach { tab ->
                val active=selected==tab
                Surface(onClick={onSelect(tab)},modifier=Modifier.weight(1f).heightIn(min=42.dp),
                    shape=RoundedCornerShape(15.dp),
                    color=if(active)aqua.copy(alpha=.17f) else Color.Transparent,
                    border=if(active)BorderStroke(1.dp,aqua) else null) {
                    Box(contentAlignment=Alignment.Center) {
                        Row(verticalAlignment=Alignment.CenterVertically,
                            horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                            Text(tab,style=MaterialTheme.typography.labelMedium,maxLines=1,
                                fontWeight=if(active)FontWeight.Bold else FontWeight.Normal)
                            if(tab=="Reviews" && count>0) {
                                Surface(shape=CircleShape,color=violet) {
                                    Text(count.toString(),Modifier.padding(horizontal=5.dp,vertical=1.dp),
                                        style=MaterialTheme.typography.labelSmall,color=Color(0xFF200E2F))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
