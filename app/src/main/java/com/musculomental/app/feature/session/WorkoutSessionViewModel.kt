package com.musculomental.app.feature.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musculomental.app.domain.PerformedSetStatus
import com.musculomental.app.domain.RestInterval
import com.musculomental.app.domain.RestTimerPreferences
import com.musculomental.app.domain.RestTimerRepository
import com.musculomental.app.domain.SessionStatus
import com.musculomental.app.domain.SetEntryErrors
import com.musculomental.app.domain.WorkoutPlan
import com.musculomental.app.domain.WorkoutSession
import com.musculomental.app.domain.WorkoutSessionRepository
import com.musculomental.app.domain.validateSessionFeedback
import com.musculomental.app.domain.validateSetEntry
import com.musculomental.app.domain.TimerAlertMode
import com.musculomental.app.domain.TimerCountMode
import com.musculomental.app.domain.TimerStartMode
import com.musculomental.app.domain.TimerState
import com.musculomental.app.domain.validateTimerPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class SetDraft(val repetitions: String = "", val load: String = "", val errors: SetEntryErrors = SetEntryErrors())

data class WorkoutSessionState(
    val loading: Boolean = true,
    val sessions: List<WorkoutSession> = emptyList(),
    val drafts: Map<Long, SetDraft> = emptyMap(),
    val openSessionId: Long? = null,
    val note: String = "",
    val effort: Int? = null,
    val discomfort: Int? = null,
    val feedbackError: String? = null,
    val busy: Boolean = false,
    val error: Boolean = false,
    val operationError: String? = null,
    val startedId: Long? = null,
    val completedId: Long? = null,
    val closed: Boolean = false,
    val timers: List<RestInterval> = emptyList(),
    val timerPreferences: RestTimerPreferences = RestTimerPreferences(),
    val now: Long = System.currentTimeMillis(),
    val alertSetId: Long? = null,
    val correctionInputs: Map<Long, String> = emptyMap(),
    val timerPreferenceError: String? = null,
    val timerDefaultInput: String = "60",
    val correctionMessage: String? = null,
    val correctedSetId: Long? = null,
    val deletedSessionId: Long? = null,
)

class WorkoutSessionViewModel(
    private val repository: WorkoutSessionRepository,
    private val restRepository: RestTimerRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(WorkoutSessionState())
    val state: StateFlow<WorkoutSessionState> = _state.asStateFlow()

    private val alerting = mutableSetOf<Long>()

    init {
        reload()
        viewModelScope.launch { restRepository.observeTimers().collect { timers -> _state.update { it.copy(timers = timers) } } }
        viewModelScope.launch { restRepository.observePreferences().collect { preferences -> _state.update { it.copy(timerPreferences = preferences, timerDefaultInput = preferences.defaultSeconds.toString()) } } }
        viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                _state.update { it.copy(now = now) }
                state.value.timers.firstOrNull { it.state == TimerState.RUNNING && !it.alerted && it.elapsedAt(now) >= it.targetSeconds && alerting.add(it.performedSetId) }?.let { timer ->
                    try { restRepository.markAlerted(timer.performedSetId); _state.update { it.copy(alertSetId = timer.performedSetId) } }
                    finally { alerting.remove(timer.performedSetId) }
                }
                delay(500)
            }
        }
    }

    fun reload() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = false) }
            try {
                repository.observeSessions().collect { sessions ->
                    _state.update { current ->
                        val additions = sessions.flatMap { it.exercises }.flatMap { exercise ->
                            exercise.sets.map { set ->
                                set.id to SetDraft(
                                    repetitions = set.repetitions?.toString() ?: exercise.plannedRepetitions.toString(),
                                    load = set.loadKg?.let(::formatLoad) ?: exercise.plannedLoadKg?.let(::formatLoad).orEmpty(),
                                )
                            }
                        }.toMap()
                        current.copy(loading = false, sessions = sessions, drafts = additions + current.drafts, error = false)
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _state.update { it.copy(loading = false, error = true) } }
        }
    }

    fun session(id: Long): WorkoutSession? = state.value.sessions.firstOrNull { it.id == id }
    fun activeSession(): WorkoutSession? = state.value.sessions.firstOrNull { it.status == SessionStatus.IN_PROGRESS || it.status == SessionStatus.PAUSED }

    fun start(plan: WorkoutPlan) = launchOperation {
        val id = repository.start(plan)
        _state.update { it.copy(startedId = id) }
    }
    fun consumeStarted() = _state.update { it.copy(startedId = null) }
    fun consumeCompleted() = _state.update { it.copy(completedId = null) }
    fun consumeClosed() = _state.update { it.copy(closed = false) }
    fun consumeDeleted() = _state.update { it.copy(deletedSessionId = null) }

    fun open(id: Long) {
        if (state.value.openSessionId == id) return
        val session = session(id) ?: return
        _state.update { it.copy(openSessionId = id, note = session.note, effort = session.effort, discomfort = session.discomfort, feedbackError = null) }
        if (session.status == SessionStatus.PAUSED) launchOperation { repository.setPaused(id, false) }
    }

    fun openCorrection(id: Long) {
        val session = session(id) ?: return
        if (session.status != SessionStatus.COMPLETED) return
        _state.update {
            it.copy(
                openSessionId = id,
                note = session.note,
                effort = session.effort,
                discomfort = session.discomfort,
                feedbackError = null,
                correctionMessage = null,
                correctedSetId = null,
            )
        }
    }

    fun changeSet(setId: Long, field: String, value: String) = _state.update { current ->
        val old = current.drafts[setId] ?: SetDraft()
        val changed = if (field == "repetitions") old.copy(repetitions = value, errors = old.errors.copy(repetitions = null)) else old.copy(load = value, errors = old.errors.copy(load = null))
        current.copy(drafts = current.drafts + (setId to changed), operationError = null)
    }

    fun copyPrevious(sessionId: Long, exerciseId: Long, setNumber: Int) {
        val exercise = session(sessionId)?.exercises?.firstOrNull { it.id == exerciseId } ?: return
        val previous = exercise.sets.firstOrNull { it.setNumber == setNumber - 1 && it.status == PerformedSetStatus.COMPLETED } ?: return
        val target = exercise.sets.firstOrNull { it.setNumber == setNumber } ?: return
        _state.update { it.copy(drafts = it.drafts + (target.id to SetDraft(previous.repetitions?.toString().orEmpty(), previous.loadKg?.let(::formatLoad).orEmpty()))) }
    }

    fun saveSet(setId: Long) {
        val draft = state.value.drafts[setId] ?: SetDraft()
        val errors = validateSetEntry(draft.repetitions, draft.load)
        if (!errors.isValid) { _state.update { it.copy(drafts = it.drafts + (setId to draft.copy(errors = errors))) }; return }
        val details = state.value.sessions.flatMap { it.exercises }.firstNotNullOfOrNull { exercise -> exercise.sets.firstOrNull { it.id == setId }?.let { exercise to it } }
        val wasCompleted = details?.second?.status == PerformedSetStatus.COMPLETED
        launchOperation {
            if (state.value.timerPreferences.finishOnNextSet) restRepository.finishActive()
            repository.saveSet(setId, draft.repetitions.toInt(), draft.load.takeIf { it.isNotBlank() }?.replace(',', '.')?.toDouble(), PerformedSetStatus.COMPLETED)
            if (!wasCompleted && state.value.timerPreferences.enabled && state.value.timerPreferences.startMode == TimerStartMode.AUTOMATIC) {
                restRepository.start(setId, details?.first?.plannedRestSeconds ?: 0, state.value.timerPreferences)
            }
        }
    }

    fun skipSet(setId: Long) = launchOperation { repository.saveSet(setId, null, null, PerformedSetStatus.SKIPPED) }

    fun correctSet(setId: Long) {
        val draft = state.value.drafts[setId] ?: SetDraft()
        val errors = validateSetEntry(draft.repetitions, draft.load)
        if (!errors.isValid) {
            _state.update { it.copy(drafts = it.drafts + (setId to draft.copy(errors = errors)), correctionMessage = null) }
            return
        }
        launchOperation {
            repository.correctSet(
                setId,
                draft.repetitions.toInt(),
                draft.load.takeIf { it.isNotBlank() }?.replace(',', '.')?.toDouble(),
                PerformedSetStatus.COMPLETED,
            )
            _state.update { it.copy(correctionMessage = "Correção da série salva.", correctedSetId = setId) }
        }
    }

    fun correctAsSkipped(setId: Long) = launchOperation {
        repository.correctSet(setId, null, null, PerformedSetStatus.SKIPPED)
        _state.update { it.copy(correctionMessage = "Série marcada como ignorada.", correctedSetId = setId) }
    }

    fun saveFeedbackCorrection(sessionId: Long) {
        if (!validateFeedback()) return
        launchOperation {
            saveFeedback(sessionId)
            _state.update { it.copy(correctionMessage = "Avaliação do treino salva.", correctedSetId = null) }
        }
    }

    fun clearCorrectionMessage() = _state.update { it.copy(correctionMessage = null, correctedSetId = null) }

    fun move(sessionId: Long, position: Int) = launchOperation { repository.setCurrentExercise(sessionId, position) }
    fun changeNote(value: String) = _state.update { it.copy(note = value, feedbackError = null) }
    fun changeEffort(value: Int?) = _state.update { it.copy(effort = if (it.effort == value) null else value, feedbackError = null) }
    fun changeDiscomfort(value: Int?) = _state.update { it.copy(discomfort = if (it.discomfort == value) null else value, feedbackError = null) }

    fun pause(sessionId: Long) {
        if (!validateFeedback()) return
        launchOperation {
            saveFeedback(sessionId)
            state.value.timers.firstOrNull { it.state == TimerState.RUNNING }?.let { restRepository.pause(it.performedSetId) }
            repository.setPaused(sessionId, true)
            _state.update { it.copy(closed = true, openSessionId = null) }
        }
    }

    fun complete(sessionId: Long) {
        val session = session(sessionId) ?: return
        if (session.exercises.any { exercise -> exercise.sets.any { it.status == PerformedSetStatus.PLANNED } }) {
            _state.update { it.copy(operationError = "Conclua ou ignore todas as séries antes de finalizar.") }
            return
        }
        if (!validateFeedback()) return
        launchOperation {
            saveFeedback(sessionId)
            restRepository.finishActive()
            repository.complete(sessionId)
            _state.update { it.copy(completedId = sessionId, openSessionId = null) }
        }
    }

    fun cancel(sessionId: Long) = launchOperation {
        restRepository.finishActive()
        repository.cancel(sessionId)
        _state.update { it.copy(closed = true, openSessionId = null) }
    }

    fun deleteCompleted(sessionId: Long) = launchOperation {
        repository.deleteCompleted(sessionId)
        _state.update { it.copy(deletedSessionId = sessionId, openSessionId = null) }
    }

    fun clearError() = _state.update { it.copy(operationError = null) }
    fun consumeTimerAlert() = _state.update { it.copy(alertSetId = null) }

    fun startRest(setId: Long, plannedSeconds: Int) = launchOperation { restRepository.start(setId, plannedSeconds, state.value.timerPreferences) }
    fun pauseRest(setId: Long) = launchOperation { restRepository.pause(setId) }
    fun resumeRest(setId: Long) = launchOperation { restRepository.resume(setId) }
    fun adjustRest(setId: Long, seconds: Int) = launchOperation { restRepository.adjust(setId, seconds) }
    fun finishRest(setId: Long) = launchOperation { restRepository.finish(setId) }
    fun deleteRest(setId: Long) = launchOperation { restRepository.delete(setId) }
    fun changeCorrection(setId: Long, value: String) = _state.update { it.copy(correctionInputs = it.correctionInputs + (setId to value), operationError = null) }
    fun correctRest(setId: Long) {
        val seconds = state.value.correctionInputs[setId]?.toIntOrNull()
        if (seconds == null || seconds !in 0..7200) { _state.update { it.copy(operationError = "Informe um intervalo realizado de 0 a 7200 segundos.") }; return }
        launchOperation { restRepository.finish(setId, seconds) }
    }

    fun updateTimerPreference(field: String, value: String) {
        val current = state.value.timerPreferences
        val changed = when (field) {
            "enabled" -> current.copy(enabled = value.toBoolean())
            "startMode" -> current.copy(startMode = TimerStartMode.valueOf(value))
            "countMode" -> current.copy(countMode = TimerCountMode.valueOf(value))
            "alertMode" -> current.copy(alertMode = TimerAlertMode.valueOf(value))
            "recordActual" -> current.copy(recordActual = value.toBoolean())
            "finishOnNextSet" -> current.copy(finishOnNextSet = value.toBoolean())
            "defaultSeconds" -> current.copy(defaultSeconds = value.toIntOrNull() ?: 0)
            else -> current
        }
        val error = validateTimerPreferences(changed)
        _state.update { it.copy(timerPreferences = changed, timerPreferenceError = error, timerDefaultInput = if (field == "defaultSeconds") value else it.timerDefaultInput) }
        if (error == null) launchOperation { restRepository.savePreferences(changed) }
    }

    private fun validateFeedback(): Boolean {
        val error = validateSessionFeedback(state.value.note, state.value.effort, state.value.discomfort)
        _state.update { it.copy(feedbackError = error) }
        return error == null
    }
    private suspend fun saveFeedback(sessionId: Long) = repository.saveFeedback(sessionId, state.value.note, state.value.effort, state.value.discomfort)

    private fun launchOperation(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, operationError = null) }
            try { block(); _state.update { it.copy(busy = false) } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { _state.update { it.copy(busy = false, operationError = error.message ?: "Não foi possível concluir a operação.") } }
        }
    }

    private fun formatLoad(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
}
