package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import com.kedevjimmy.pesatrack.domain.planning.VoiceBillDraft
import com.kedevjimmy.pesatrack.domain.planning.VoiceGoalDraft
import com.kedevjimmy.pesatrack.domain.planning.VoicePlanDraft
import com.kedevjimmy.pesatrack.planning.BillSchedules
import java.util.UUID

data class VoicePlanSaveResult(
    val ruleId: String,
    val displayName: String
)

class VoicePlanningRepository(
    context: Context
) {
    private val app =
        context.applicationContext
    private val planningRules =
        PlanningRuleRepository(app)
    private val goalMetadata =
        SavingGoalMetadataStore.get(app)
    private val billSchedules =
        BillSchedules(app)
    private val transactionStore =
        TransactionStore.get(app)

    fun save(
        draft: VoicePlanDraft
    ): VoicePlanSaveResult =
        when (draft) {
            is VoiceGoalDraft ->
                saveGoal(draft)
            is VoiceBillDraft ->
                saveBill(draft)
        }

    private fun saveGoal(
        draft: VoiceGoalDraft
    ): VoicePlanSaveResult {
        // Voice-created goals can be created before the user knows which
        // savings account/payee will fund them. Use a deliberately impossible
        // local matcher so no real transaction can be auto-linked by accident.
        // The normal Edit goal flow can later replace this with a real payee.
        val nonMatchingPayee =
            "__PESATRACK_VOICE_GOAL__" +
                UUID.randomUUID()
                    .toString()

        val saved =
            planningRules.save(
                name = draft.name,
                payee = nonMatchingPayee,
                target =
                    draft.monthlyContributionMinor,
                bill = false,
                existing = null
            )

        goalMetadata.save(
            ruleId = saved.id,
            totalTargetMinor =
                draft.totalTargetMinor,
            targetDateEpochMs =
                draft.targetDateEpochMs
        )

        return VoicePlanSaveResult(
            ruleId = saved.id,
            displayName =
                draft.name
        )
    }

    private fun saveBill(
        draft: VoiceBillDraft
    ): VoicePlanSaveResult {
        val saved =
            planningRules.save(
                name = draft.name,
                payee = draft.payee,
                target =
                    draft.amountMinor,
                bill = true,
                existing = null
            )

        // Apply the same trusted-history linking used by the manual bill
        // editor, so a bill already paid this month does not reappear as due.
        transactionStore.reapplyRule(
            rule = saved,
            rebuildPayCycles = true
        )

        billSchedules.saveSchedule(
            ruleId = saved.id,
            cadence = draft.cadence,
            dueDay = draft.dueDay,
            dueDayOfWeek =
                draft.dueDayOfWeek,
            enabled = false
        )

        return VoicePlanSaveResult(
            ruleId = saved.id,
            displayName =
                draft.name
        )
    }
}
