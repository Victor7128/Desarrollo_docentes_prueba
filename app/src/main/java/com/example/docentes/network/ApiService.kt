package com.example.docentes.network

import com.example.docentes.models.*
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @GET("bimesters/full")
    fun getBimesters(): Call<List<BimesterData>>

    // ========== ENDPOINTS EXISTENTES ==========

    // Endpoints de Grados
    @POST("bimesters/{bimester_id}/grades")
    fun createGrade(
        @Path("bimester_id") bimesterId: Int,
        @Body body: CreateGradeRequest
    ): Call<Grade>

    @DELETE("grades/{grade_id}")
    fun deleteGrade(@Path("grade_id") gradeId: Int): Call<Void>

    @GET("bimesters/{bimester_id}/grades")
    fun listGrades(@Path("bimester_id") bimesterId: Int): Call<List<Grade>>

    @GET("grades/{grade_id}")
    fun getGrade(@Path("grade_id") gradeId: Int): Call<Grade>

    // Endpoints de Secciones
    @POST("grades/{grade_id}/sections")
    fun createSection(
        @Path("grade_id") gradeId: Int,
        @Body body: CreateSectionRequest
    ): Call<Section>

    @DELETE("sections/{section_id}")
    fun deleteSection(@Path("section_id") sectionId: Int): Call<Void>

    @GET("grades/{grade_id}/sections")
    fun listSections(@Path("grade_id") gradeId: Int): Call<List<Section>>

    @GET("sections/{section_id}")
    fun getSection(@Path("section_id") sectionId: Int): Call<Section>

    // Endpoints de Estudiantes
    @POST("sections/{section_id}/students")
    fun createStudent(
        @Path("section_id") sectionId: Int,
        @Body body: CreateStudentRequest
    ): Call<Student>

    @GET("sections/{section_id}/students")
    fun listStudents(@Path("section_id") sectionId: Int): Call<List<Student>>

    @PUT("students/{student_id}")
    fun updateStudent(
        @Path("student_id") studentId: Int,
        @Body body: CreateStudentRequest
    ): Call<Student>

    @DELETE("students/{student_id}")
    fun deleteStudent(@Path("student_id") studentId: Int): Call<Void>

    // Importación de estudiantes
    @POST("sections/{section_id}/students/import")
    fun importStudentsJson(
        @Path("section_id") sectionId: Int,
        @Body body: BatchStudentsRequest
    ): Call<ImportStudentsResponse>

    @Multipart
    @POST("sections/{section_id}/students/import_csv")
    fun importStudentsCsv(
        @Path("section_id") sectionId: Int,
        @Part file: MultipartBody.Part
    ): Call<ImportStudentsResponse>

    @Multipart
    @POST("sections/{section_id}/students/import_txt")
    fun importStudentsTxt(
        @Path("section_id") sectionId: Int,
        @Part file: MultipartBody.Part
    ): Call<ImportStudentsResponse>

    // ========== SESSIONS ==========

    @GET("sections/{sectionId}/sessions")
    suspend fun getSessions(
        @Path("sectionId") sectionId: Int
    ): List<Session>

    @POST("sections/{sectionId}/sessions")
    suspend fun createSession(
        @Path("sectionId") sectionId: Int,
        @Body request: NewSessionRequest
    ): Session

    @GET("sessions/{sessionId}")
    suspend fun getSession(
        @Path("sessionId") sessionId: Int
    ): Session

    @PUT("sessions/{sessionId}")
    suspend fun updateSession(
        @Path("sessionId") sessionId: Int,
        @Body request: UpdateSessionRequest
    ): Session

    @DELETE("sessions/{sessionId}")
    suspend fun deleteSession(
        @Path("sessionId") sessionId: Int
    ): Response<Unit>

    // ========== PRODUCTS ==========

    @GET("sessions/{sessionId}/products")
    suspend fun getProducts(
        @Path("sessionId") sessionId: Int
    ): List<Product>

    @POST("sessions/{sessionId}/products")
    suspend fun createProduct(
        @Path("sessionId") sessionId: Int,
        @Body request: Map<String, String>
    ): Product

    @PUT("products/{productId}")
    suspend fun updateProduct(
        @Path("productId") productId: Int,
        @Body request: UpdateProductRequest
    ): Product

    @DELETE("products/{productId}")
    suspend fun deleteProduct(
        @Path("productId") productId: Int
    ): Response<Unit>

    // ========== COMPETENCIES ==========

    @GET("sessions/{sessionId}/competencies")
    suspend fun getCompetencies(
        @Path("sessionId") sessionId: Int
    ): List<Competency>

    @POST("sessions/{sessionId}/competencies")
    suspend fun createCompetency(
        @Path("sessionId") sessionId: Int,
        @Body request: NewCompetencyRequest
    ): Competency

    @PUT("competencies/{competencyId}")
    suspend fun updateCompetency(
        @Path("competencyId") competencyId: Int,
        @Body request: UpdateCompetencyRequest
    ): Competency

    @DELETE("competencies/{competencyId}")
    suspend fun deleteCompetency(
        @Path("competencyId") competencyId: Int
    ): Response<Unit>

    // ========== ABILITIES ==========

    @GET("competencies/{competencyId}/abilities")
    suspend fun getAbilities(
        @Path("competencyId") competencyId: Int
    ): List<Ability>

    @GET("abilities/{abilityId}")
    suspend fun getAbility(
        @Path("abilityId") abilityId: Int
    ): Ability

    @POST("competencies/{competencyId}/abilities")
    suspend fun createAbility(
        @Path("competencyId") competencyId: Int,
        @Body request: Map<String, String>
    ): Ability

    @PUT("abilities/{abilityId}")
    suspend fun updateAbility(
        @Path("abilityId") abilityId: Int,
        @Body request: UpdateAbilityRequest
    ): Ability

    @DELETE("abilities/{abilityId}")
    suspend fun deleteAbility(
        @Path("abilityId") abilityId: Int
    ): Response<Unit>

    // ========== CRITERIA ==========

    @GET("abilities/{abilityId}/criteria")
    suspend fun getCriteria(
        @Path("abilityId") abilityId: Int
    ): List<Criterion>

    @POST("abilities/{abilityId}/criteria")
    suspend fun createCriterion(
        @Path("abilityId") abilityId: Int,
        @Body request: Map<String, String>
    ): Criterion

    @PUT("criteria/{criterionId}")
    suspend fun updateCriterion(
        @Path("criterionId") criterionId: Int,
        @Body request: UpdateCriterionRequest
    ): Criterion

    @DELETE("criteria/{criterionId}")
    suspend fun deleteCriterion(
        @Path("criterionId") criterionId: Int
    ): Response<Unit>

    // ========== NUEVOS ENDPOINTS DE EVALUACIÓN ==========

    @GET("evaluation/context")
    suspend fun getEvaluationContext(
        @Query("session_id") sessionId: Int,
        @Query("competency_id") competencyId: Int,
        @Query("product_id") productId: Int
    ): EvaluationContext

    @PUT("evaluation/value")
    suspend fun upsertEvaluation(
        @Body request: EvalValueRequest
    ): EvaluationResponse

    @GET("evaluation/item")
    suspend fun getEvaluationItem(
        @Query("session_id") sessionId: Int,
        @Query("competency_id") competencyId: Int,
        @Query("ability_id") abilityId: Int,
        @Query("criterion_id") criterionId: Int,
        @Query("product_id") productId: Int,
        @Query("student_id") studentId: Int
    ): EvaluationItem

    @DELETE("evaluation/item")
    suspend fun deleteEvaluationItem(
        @Query("session_id") sessionId: Int,
        @Query("competency_id") competencyId: Int,
        @Query("ability_id") abilityId: Int,
        @Query("criterion_id") criterionId: Int,
        @Query("product_id") productId: Int,
        @Query("student_id") studentId: Int
    ): Response<Unit>

    @GET("sessions/{sessionId}/products/{productId}/competencies/{competencyId}/matrix")
    suspend fun getEvaluationMatrix(
        @Path("sessionId") sessionId: Int,
        @Path("productId") productId: Int,
        @Path("competencyId") competencyId: Int
    ): MatrixResponse

    @GET("sections/{sectionId}/consolidado")
    suspend fun getConsolidatedSection(
        @Path("sectionId") sectionId: Int
    ): ConsolidatedSectionResponse

    // ========== ✅ ENDPOINTS AUXILIARES PARA ESTUDIANTES ==========

    @GET("sections/{sectionId}/students")
    suspend fun getStudentsByClassroom(
        @Path("sectionId") sectionId: Int
    ): Response<ApiResponse<List<Student>>>

    @GET("competencies/{competencyId}/criteria")
    suspend fun getCriteriaByCompetency(
        @Path("competencyId") competencyId: Int
    ): Response<ApiResponse<List<Criterion>>>

    @GET("sessions/{sessionId}/competencies/{competencyId}/products/{productId}/evaluations")
    suspend fun getEvaluations(
        @Path("sessionId") sessionId: Int,
        @Path("competencyId") competencyId: Int,
        @Path("productId") productId: Int
    ): Response<ApiResponse<List<EvaluationValue>>>
}