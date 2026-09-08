package com.musculomental.app.data

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import com.musculomental.app.domain.Equipment
import com.musculomental.app.domain.Exercise
import com.musculomental.app.domain.ExerciseLibraryRepository
import com.musculomental.app.domain.ExerciseMuscle
import com.musculomental.app.domain.Muscle
import com.musculomental.app.domain.MuscleRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "muscle")
data class MuscleEntity(@PrimaryKey val id: String, val commonName: String, val anatomicalName: String, val region: String, val function: String)

@Entity(tableName = "exercise")
data class ExerciseEntity(@PrimaryKey val id: String, val name: String, val equipment: String, val instructions: String, val commonErrors: String)

@Entity(
    tableName = "exercise_muscle",
    primaryKeys = ["exerciseId", "muscleId"],
    foreignKeys = [
        ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = MuscleEntity::class, parentColumns = ["id"], childColumns = ["muscleId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("muscleId")],
)
data class ExerciseMuscleEntity(val exerciseId: String, val muscleId: String, val role: String)

data class ExerciseWithMuscles(
    @Embedded val exercise: ExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "id", associateBy = Junction(ExerciseMuscleEntity::class, parentColumn = "exerciseId", entityColumn = "muscleId"))
    val muscles: List<MuscleEntity>,
    @Relation(parentColumn = "id", entityColumn = "exerciseId") val relations: List<ExerciseMuscleEntity>,
)

@Dao
interface ExerciseDao {
    @Transaction @Query("SELECT * FROM exercise ORDER BY name") fun observeAll(): Flow<List<ExerciseWithMuscles>>
    @Query("SELECT COUNT(*) FROM exercise") suspend fun count(): Int
    @Upsert suspend fun insertMuscles(items: List<MuscleEntity>)
    @Upsert suspend fun insertExercises(items: List<ExerciseEntity>)
    @Upsert suspend fun insertRelations(items: List<ExerciseMuscleEntity>)
}

class LocalExerciseLibraryRepository(private val dao: ExerciseDao) : ExerciseLibraryRepository {
    override fun observeExercises(): Flow<List<Exercise>> = flow {
        if (dao.count() == 0) seed()
        emitAll(dao.observeAll().map { rows -> rows.map { it.toDomain() } })
    }

    private suspend fun seed() {
        dao.insertMuscles(muscles)
        dao.insertExercises(exercises)
        dao.insertRelations(relations)
    }

    private fun ExerciseWithMuscles.toDomain(): Exercise {
        val roles = relations.associate { it.muscleId to MuscleRole.valueOf(it.role) }
        return Exercise(
            exercise.id, exercise.name, Equipment.valueOf(exercise.equipment), exercise.instructions.split("|"), exercise.commonErrors.split("|"),
            muscles.map { muscle -> ExerciseMuscle(Muscle(muscle.id, muscle.commonName, muscle.anatomicalName, muscle.region, muscle.function), roles.getValue(muscle.id)) }.sortedBy { it.role },
        )
    }

    private companion object Seed {
        val muscles = listOf(
            MuscleEntity("peitoral-maior", "Peitoral maior", "Pectoralis major", "Peito", "Aduz e roda internamente o braço; sua porção clavicular também auxilia a flexão do ombro."),
            MuscleEntity("deltoide-anterior", "Deltoide anterior", "Deltoideus pars clavicularis", "Ombros", "Auxilia a flexão e a rotação interna do ombro."),
            MuscleEntity("deltoide-lateral", "Deltoide lateral", "Deltoideus pars acromialis", "Ombros", "Participa principalmente da abdução do braço."),
            MuscleEntity("triceps", "Tríceps braquial", "Triceps brachii", "Braços", "Estende o cotovelo; a cabeça longa também atua no ombro."),
            MuscleEntity("biceps", "Bíceps braquial", "Biceps brachii", "Braços", "Flexiona o cotovelo e participa da supinação do antebraço."),
            MuscleEntity("braquial", "Braquial", "Brachialis", "Braços", "É um importante flexor do cotovelo."),
            MuscleEntity("braquiorradial", "Braquiorradial", "Brachioradialis", "Antebraços", "Flexiona o cotovelo, com maior vantagem em pegada neutra."),
            MuscleEntity("quadriceps", "Quadríceps", "Quadriceps femoris", "Pernas", "Estende o joelho; o reto femoral também auxilia a flexão do quadril."),
            MuscleEntity("gluteo-maximo", "Glúteo máximo", "Gluteus maximus", "Glúteos", "Estende e roda externamente o quadril."),
            MuscleEntity("posteriores-coxa", "Posteriores de coxa", "Hamstrings", "Pernas", "Flexionam o joelho e auxiliam a extensão do quadril, exceto a cabeça curta do bíceps femoral."),
        )
        fun exercise(id: String, name: String, equipment: Equipment, instructions: String, errors: String) = ExerciseEntity(id, name, equipment.name, instructions, errors)
        val exercises = listOf(
            exercise("supino-reto", "Supino reto", Equipment.BARBELL, "Deite com apoio estável|Desça a barra com controle em direção ao peito|Empurre mantendo os punhos alinhados", "Perder a estabilidade dos ombros|Quicar a barra no peito"),
            exercise("desenvolvimento", "Desenvolvimento de ombros", Equipment.DUMBBELL, "Mantenha tronco e pés estáveis|Eleve os halteres sem perder o controle|Retorne até uma amplitude confortável", "Arquear excessivamente a lombar|Usar impulso do tronco"),
            exercise("elevacao-lateral", "Elevação lateral", Equipment.DUMBBELL, "Mantenha os cotovelos levemente flexionados|Eleve os braços lateralmente com controle|Retorne sem deixar os pesos caírem", "Encolher os ombros|Usar balanço excessivo"),
            exercise("leg-press", "Leg press", Equipment.MACHINE, "Apoie os pés de forma estável|Flexione joelhos e quadris com controle|Empurre sem travar bruscamente os joelhos", "Perder contato do quadril com o encosto|Deixar os joelhos colapsarem para dentro"),
            exercise("agachamento", "Agachamento", Equipment.BODY_WEIGHT, "Mantenha os pés estáveis|Flexione quadris e joelhos em amplitude controlada|Suba mantendo os joelhos alinhados aos pés", "Perder o equilíbrio do pé|Forçar uma amplitude sem controle"),
            exercise("rosca-direta", "Rosca direta", Equipment.BARBELL, "Mantenha os braços próximos ao tronco|Flexione os cotovelos sem balançar o corpo|Desça a barra com controle", "Projetar o tronco para gerar impulso|Avançar excessivamente os cotovelos"),
            exercise("rosca-inversa", "Rosca inversa", Equipment.BARBELL, "Use pegada pronada|Flexione os cotovelos mantendo os punhos alinhados|Retorne com controle", "Dobrar os punhos|Usar balanço do tronco"),
            exercise("triceps-halter", "Tríceps francês com halter", Equipment.DUMBBELL, "Estabilize tronco e braços|Flexione os cotovelos levando o peso para trás|Estenda os cotovelos com controle", "Abrir excessivamente os cotovelos|Arquear a lombar"),
        )
        fun relation(exercise: String, muscle: String, role: MuscleRole) = ExerciseMuscleEntity(exercise, muscle, role.name)
        val relations = listOf(
            relation("supino-reto", "peitoral-maior", MuscleRole.PRIMARY), relation("supino-reto", "triceps", MuscleRole.ASSISTANT), relation("supino-reto", "deltoide-anterior", MuscleRole.ASSISTANT),
            relation("desenvolvimento", "deltoide-anterior", MuscleRole.PRIMARY), relation("desenvolvimento", "triceps", MuscleRole.ASSISTANT), relation("desenvolvimento", "deltoide-lateral", MuscleRole.ASSISTANT),
            relation("elevacao-lateral", "deltoide-lateral", MuscleRole.PRIMARY), relation("leg-press", "quadriceps", MuscleRole.PRIMARY), relation("leg-press", "gluteo-maximo", MuscleRole.ASSISTANT), relation("leg-press", "posteriores-coxa", MuscleRole.ASSISTANT),
            relation("agachamento", "quadriceps", MuscleRole.PRIMARY), relation("agachamento", "gluteo-maximo", MuscleRole.PRIMARY), relation("agachamento", "posteriores-coxa", MuscleRole.ASSISTANT),
            relation("rosca-direta", "biceps", MuscleRole.PRIMARY), relation("rosca-direta", "braquial", MuscleRole.ASSISTANT), relation("rosca-inversa", "braquiorradial", MuscleRole.PRIMARY), relation("rosca-inversa", "braquial", MuscleRole.PRIMARY), relation("rosca-inversa", "biceps", MuscleRole.ASSISTANT), relation("triceps-halter", "triceps", MuscleRole.PRIMARY),
        )
    }
}
