package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kedevjimmy.pesatrack.data.local.ObservedInvestmentPosition
import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val turquoise = Color(0xFF42E5D2)
private val danger = Color(0xFFFF536D)
private val amber = Color(0xFFFFC55C)
private val violet = Color(0xFFB7A4FF)
private val blue = Color(0xFF399CFF)

@Composable
private fun DesignBCard(
    modifier:Modifier=Modifier,
    accent:Color=turquoise,
    content:@Composable ColumnScope.()->Unit
) {
    Surface(modifier=modifier, shape=RoundedCornerShape(18.dp),
        color=MaterialTheme.colorScheme.surface,
        border=BorderStroke(1.dp,accent.copy(alpha=.43f))) {
        Column(Modifier.padding(11.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp),content=content)
    }
}

@Composable
internal fun HomeBGreeting(name:String) {
    val hour=java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting=when(hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    Column(verticalArrangement=Arrangement.spacedBy(3.dp)) {
        Text(greeting+name.takeIf(String::isNotBlank)?.let { ", "+it }.orEmpty(),
            style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
        Text("Here's what's happening with your money.",
            color=MaterialTheme.colorScheme.onSurfaceVariant,
            style=MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun HomeBPeriod(month:String,daysLeft:Int?,onClick:()->Unit) {
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp),
        verticalAlignment=Alignment.CenterVertically) {
        Surface(onClick=onClick, modifier=Modifier.weight(1f).height(40.dp),
            color=MaterialTheme.colorScheme.surface,
            shape=RoundedCornerShape(13.dp),
            border=BorderStroke(1.dp,blue.copy(alpha=.7f))) {
            Row(Modifier.padding(horizontal=10.dp),verticalAlignment=Alignment.CenterVertically) {
                Icon(Icons.Rounded.CalendarMonth,null,Modifier.size(18.dp),tint=turquoise)
                Spacer(Modifier.width(7.dp))
                Text(month,Modifier.weight(1f),maxLines=1,
                    style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.SemiBold)
                Icon(Icons.Rounded.ChevronRight,"Choose month",Modifier.size(18.dp))
            }
        }
        if(daysLeft!=null) Surface(
            color=turquoise.copy(alpha=.12f),
            shape=RoundedCornerShape(13.dp),
            border=BorderStroke(1.dp,turquoise.copy(alpha=.5f))) {
            Text(daysLeft.toString()+" days left",
                Modifier.padding(horizontal=9.dp,vertical=11.dp),
                color=turquoise,style=MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
internal fun HomeBHealth(health: HomeFinancialSignals, onDetails: () -> Unit) {
    DesignBCard(accent=violet) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text("Financial health", modifier=Modifier.weight(1f),
                style=MaterialTheme.typography.titleSmall,
                fontWeight=FontWeight.Bold)
            Surface(onClick=onDetails,shape=RoundedCornerShape(10.dp),
                color=violet.copy(alpha=.12f)) {
                Row(Modifier.padding(horizontal=9.dp,vertical=6.dp),
                    verticalAlignment=Alignment.CenterVertically) {
                    Text("Guidance",fontSize=11.sp,color=violet,fontWeight=FontWeight.Bold)
                    Icon(Icons.Rounded.ChevronRight,null,Modifier.size(15.dp),tint=violet)
                }
            }
        }
        Row(Modifier.fillMaxWidth().clickable(onClick=onDetails),
            horizontalArrangement=Arrangement.spacedBy(12.dp),
            verticalAlignment=Alignment.CenterVertically) {
            val stateColor=when(health.status) {
                "Needs attention"->amber
                "Worth reviewing"->amber
                "On track"->turquoise
                else->violet
            }
            Box(Modifier.size(100.dp),contentAlignment=Alignment.Center) {
                Canvas(Modifier.matchParentSize()) {
                    val stroke=8.dp.toPx()
                    drawArc(stateColor.copy(alpha=.19f),130f,280f,false,
                        style=Stroke(stroke,cap=StrokeCap.Round))
                    health.score?.let { score ->
                        drawArc(Brush.sweepGradient(listOf(stateColor.copy(alpha=.7f),
                            stateColor,stateColor)),130f,280f*score/100f,false,
                            style=Stroke(stroke,cap=StrokeCap.Round))
                    }
                }
                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    Text(health.score?.toString()?:"—",fontSize=29.sp,
                        color=stateColor,fontWeight=FontWeight.Bold)
                    Text(if(health.score==null) "No full score" else "/100",
                        fontSize=10.sp,color=stateColor)
                }
            }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Surface(Modifier.size(9.dp),shape=CircleShape,color=stateColor){}
                    Spacer(Modifier.width(7.dp))
                    Text(health.status,color=stateColor,
                        style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,
                        modifier=Modifier.weight(1f))
                    Icon(Icons.Rounded.ChevronRight,"Explain financial indicators",
                        Modifier.size(18.dp))
                }
                Text(health.explanation,style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines=3,overflow=TextOverflow.Ellipsis)
            }
        }
        val tints=listOf(turquoise,blue,amber,violet)
        val icons=listOf(Icons.Rounded.AccountBalanceWallet,Icons.Rounded.Savings,
            Icons.Rounded.AccountBalance,Icons.Rounded.Timeline)
        health.indicators.chunked(2).forEachIndexed { rowIndex, pair ->
            Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                pair.forEachIndexed { index, signal ->
                    val i=rowIndex*2+index
                    Surface(onClick=onDetails,modifier=Modifier.weight(1f),
                        shape=RoundedCornerShape(12.dp),
                        color=tints[i].copy(alpha=.07f),
                        border=BorderStroke(1.dp,tints[i].copy(alpha=.23f))) {
                        Column(Modifier.padding(9.dp),
                            verticalArrangement=Arrangement.spacedBy(5.dp)) {
                            Row(verticalAlignment=Alignment.CenterVertically) {
                                Icon(icons[i],null,Modifier.size(18.dp),tint=tints[i])
                                Spacer(Modifier.width(6.dp))
                                Text(signal.title,style=MaterialTheme.typography.labelMedium,
                                    fontWeight=FontWeight.SemiBold,maxLines=1)
                            }
                            Text(signal.score?.let { it.toString()+"/100" }?:"More evidence needed",
                                color=tints[i],fontWeight=FontWeight.Bold,fontSize=12.sp,
                                maxLines=1)
                            signal.score?.let { value ->
                                LinearProgressIndicator(progress={value/100f},
                                    modifier=Modifier.fillMaxWidth().height(3.dp),
                                    color=tints[i],trackColor=tints[i].copy(alpha=.18f),
                                    drawStopIndicator={})
                            }
                        }
                    }
                }
            }
        }
        Surface(onClick=onDetails,shape=RoundedCornerShape(11.dp),
            color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.45f)) {
            Column(Modifier.fillMaxWidth().padding(10.dp),
                verticalArrangement=Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle,null,Modifier.size(17.dp),tint=violet)
                    Spacer(Modifier.width(6.dp))
                    Text("Data confidence",style=MaterialTheme.typography.labelMedium,
                        fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
                    Text(health.confidence.score?.let { it.toString()+"/100" }?:"Unknown",
                        color=violet,style=MaterialTheme.typography.labelSmall)
                }
                Text(health.confidence.reason,style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,
                    overflow=TextOverflow.Ellipsis)
            }
        }
        health.actions.firstOrNull()?.let { action ->
            Row(Modifier.fillMaxWidth().clickable(onClick=onDetails),
                verticalAlignment=Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lightbulb,null,Modifier.size(17.dp),tint=amber)
                Spacer(Modifier.width(7.dp))
                Text(action.title,style=MaterialTheme.typography.labelMedium,
                    modifier=Modifier.weight(1f),maxLines=2)
                Icon(Icons.Rounded.ChevronRight,null,Modifier.size(17.dp))
            }
        }
    }
}

@Composable
internal fun HomeBMonthly(
    isCurrent:Boolean,month:String,daysLeft:Int?,moneyIn:String,moneyOut:String,
    netFlow:String,negative:Boolean,inComparison:Int?,outComparison:Int?,
    progress:Float?,onExplain:()->Unit
) {
    DesignBCard {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text(if(isCurrent) "This month ("+month+")" else month,
                Modifier.weight(1f),
                style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold)
            if(daysLeft!=null) Text(daysLeft.toString()+" days left",
                style=MaterialTheme.typography.labelSmall,color=turquoise)
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            HomeBMoneyFigure("Money in",moneyIn,inComparison,Modifier.weight(1f))
            HomeBMoneyFigure("Money out",moneyOut,outComparison,Modifier.weight(1.15f))
            Box(Modifier.size(100.dp),contentAlignment=Alignment.Center) {
                Canvas(Modifier.matchParentSize()) {
                    drawArc(Color(0xFF314658),125f,290f,false,
                        style=Stroke(7.dp.toPx(),cap=StrokeCap.Round))
                    if(progress!=null)drawArc(
                        Brush.sweepGradient(listOf(turquoise,blue,turquoise)),
                        125f,290f*progress.coerceIn(0f,1f),false,
                        style=Stroke(7.dp.toPx(),cap=StrokeCap.Round))
                }
                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    Text("Net flow",fontSize=9.sp)
                    Text(netFlow,fontSize=9.sp,lineHeight=10.sp,maxLines=2,
                        fontWeight=FontWeight.Bold,color=if(negative)danger else turquoise,
                        textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                    IconButton(onClick=onExplain,modifier=Modifier.size(24.dp)){
                        Icon(Icons.Rounded.Info,"About net flow",
                            Modifier.size(15.dp),tint=turquoise)
                    }
                }
            }
        }
        Canvas(Modifier.fillMaxWidth().height(22.dp)) {
            val w=size.width;val h=size.height
            val p=androidx.compose.ui.graphics.Path().apply {
                moveTo(0f,h*.59f)
                cubicTo(w*.14f,-h*.2f,w*.26f,h*1.40f,w*.42f,h*.55f)
                cubicTo(w*.59f,-h*.23f,w*.73f,h*1.69f,w,h*.46f)
            }
            val area=androidx.compose.ui.graphics.Path().apply {
                addPath(p)
                lineTo(w,h)
                lineTo(0f,h)
                close()
            }
            drawPath(area,brush=Brush.verticalGradient(
                listOf(turquoise.copy(alpha=.38f),turquoise.copy(alpha=.08f)),
                startY=0f,endY=h))
            drawPath(p,turquoise.copy(alpha=.12f),
                style=Stroke(6.dp.toPx(),cap=StrokeCap.Round))
            drawPath(p,turquoise.copy(alpha=.93f),
                style=Stroke(2.dp.toPx(),cap=StrokeCap.Round))
        }
    }
}

@Composable
private fun HomeBMoneyFigure(title:String,amount:String,change:Int?,modifier:Modifier) {
    Column(modifier,verticalArrangement=Arrangement.spacedBy(5.dp)) {
        Text(title,color=MaterialTheme.colorScheme.onSurfaceVariant,
            style=MaterialTheme.typography.labelSmall)
        Text(amount,style=MaterialTheme.typography.labelLarge,
            fontWeight=FontWeight.Bold,maxLines=2,lineHeight=16.sp)
        Text(change?.let { (if(it>0)"+" else "")+it.toString()+"% vs prior" }
            ?:"No prior data",fontSize=10.sp,lineHeight=11.sp,maxLines=2,
            color=if((change?:0)>0)danger else
                MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun HomeBInsight(text:String,onClick:()->Unit) {
    Surface(onClick=onClick,
        shape=RoundedCornerShape(17.dp),
        color=Color(0xFF3B2A13),
        border=BorderStroke(1.dp,amber.copy(alpha=.58f))) {
        Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Surface(shape=CircleShape,color=amber.copy(alpha=.18f)) {
                Icon(Icons.Rounded.Lightbulb,null,Modifier.padding(10.dp).size(22.dp),
                    tint=amber)
            }
            Column(Modifier.weight(1f)) {
                Text("Spending insight",fontWeight=FontWeight.Bold,color=Color.White)
                Text(text,style=MaterialTheme.typography.bodySmall,
                    color=Color(0xFFF6E8C7),maxLines=2)
            }
            Icon(Icons.Rounded.ChevronRight,"Review spending",tint=amber)
        }
    }
}

@Composable
internal fun HomeBTile(
    title:String,amount:String,help:String,icon:ImageVector,
    tint:Color,onClick:()->Unit,modifier:Modifier=Modifier
) {
    Surface(onClick=onClick,
        modifier=modifier.heightIn(min=94.dp),
        shape=RoundedCornerShape(16.dp),
        color=MaterialTheme.colorScheme.surface,
        border=BorderStroke(1.dp,tint.copy(alpha=.33f))) {
        Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                Surface(shape=RoundedCornerShape(10.dp),color=tint.copy(alpha=.16f)) {
                    Icon(icon,null,Modifier.padding(5.dp).size(20.dp),tint=tint)
                }
                Text(title,style=MaterialTheme.typography.labelMedium,
                    maxLines=2,lineHeight=13.sp)
            }
            Text(amount,style=MaterialTheme.typography.titleSmall,
                fontWeight=FontWeight.Bold,maxLines=1,
                overflow=TextOverflow.Ellipsis)
            Text(help,color=MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize=10.sp,maxLines=2,lineHeight=12.sp)
        }
    }
}

@Composable
internal fun HomeBInvestmentsDialog(
    positions:List<ObservedInvestmentPosition>,onDismiss:()->Unit
) {
    AlertDialog(onDismissRequest=onDismiss,
        title={Text("Observed investments")},
        text={
            Column(verticalArrangement=Arrangement.spacedBy(9.dp)) {
                Text("The app has execution observations, not a verified market value or an authoritative brokerage balance.",
                    style=MaterialTheme.typography.bodySmall)
                if(positions.isEmpty()) Text("No executed investments have been detected.")
                positions.take(6).forEach { pos ->
                    Text(pos.label,fontWeight=FontWeight.Bold)
                    Text("Observed net units: "+
                        java.text.NumberFormat.getNumberInstance(Locale.US)
                            .format(pos.quantityMicrounits/1_000_000.0)+
                        " · Partial history",
                        style=MaterialTheme.typography.labelSmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if(positions.size>6) Text("More investment records are available.")
            }
        },
        confirmButton={TextButton(onClick=onDismiss){Text("Close")}}
    )
}

@Composable
internal fun HomeBRecent(
    rows:List<StoredTransaction>, category:(String)->String,
    money:(Long)->String,onOpen:(Long)->Unit,onAll:()->Unit
) {
    DesignBCard(accent=violet) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text("Recent activity",Modifier.weight(1f),
                style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold)
            TextButton(onClick=onAll,contentPadding=PaddingValues(2.dp)){
                Text("See all",color=blue,style=MaterialTheme.typography.labelMedium)
            }
        }
        if(rows.isEmpty()) Text("No transactions for this month.",
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        val locale=androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
        rows.take(3).forEachIndexed { index,row ->
            Row(Modifier.fillMaxWidth().clickable { onOpen(row.id) }
                .padding(vertical=4.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                Surface(shape=CircleShape,color=violet.copy(alpha=.18f)) {
                    Icon(Icons.Rounded.ReceiptLong,null,
                        Modifier.padding(8.dp).size(19.dp),tint=violet)
                }
                Column(Modifier.weight(1f)) {
                    Text(row.counterparty?.takeIf(String::isNotBlank)?:"Transaction",
                        style=MaterialTheme.typography.labelMedium,
                        fontWeight=FontWeight.SemiBold,maxLines=1,
                        overflow=TextOverflow.Ellipsis)
                    Text(category(row.category)+" · "+
                        SimpleDateFormat("d MMM, HH:mm",locale)
                            .format(Date(row.occurredAtEpochMs)),
                        fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines=1)
                }
                val incoming=row.direction==MoneyDirection.INCOME.name
                Text((if(incoming)"+" else if(row.direction==MoneyDirection.EXPENSE.name)"−" else "")+
                    money(row.amountMinor),fontSize=11.sp,fontWeight=FontWeight.Bold,
                    color=if(incoming)turquoise else if(row.direction==MoneyDirection.EXPENSE.name)
                        danger else MaterialTheme.colorScheme.onSurface,
                    maxLines=1)
            }
            if(index<rows.take(3).lastIndex)
                HorizontalDivider(thickness=.5.dp,
                    color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.35f))
        }
    }
}
