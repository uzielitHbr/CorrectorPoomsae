package tkd.apptkd.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mx.taekwondo.poomsae.analysis.poomsae.PoomsaeDefinition
import mx.taekwondo.poomsae.analysis.poomsae.PoomsaeLoadResult
import mx.taekwondo.poomsae.analysis.poomsae.PoomsaeLoader
import tkd.apptkd.ui.AndroidVoicePlayer
import tkd.apptkd.ui.VoicePlayer
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePoomsaeDefinition(@ApplicationContext context: Context): PoomsaeDefinition {
        val json = context.assets.open("poomsae_taegeuk1.json").bufferedReader().use { it.readText() }
        return when (val result = PoomsaeLoader.loadFromString(json)) {
            is PoomsaeLoadResult.Success -> result.definition
            is PoomsaeLoadResult.Failure -> error("Failed to load canonical poomsae: ${result.errors}")
        }
    }

    @Provides
    @Singleton
    fun provideVoicePlayer(@ApplicationContext context: Context): VoicePlayer =
        AndroidVoicePlayer(context)

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): tkd.apptkd.data.room.AppDatabase =
        tkd.apptkd.data.room.AppDatabase.buildDatabase(context)

    @Provides
    @Singleton
    fun provideSessionDao(database: tkd.apptkd.data.room.AppDatabase): tkd.apptkd.data.room.SessionDao =
        database.sessionDao()

    @Provides
    @Singleton
    fun provideSessionDraftDao(database: tkd.apptkd.data.room.AppDatabase): tkd.apptkd.data.room.SessionDraftDao =
        database.sessionDraftDao()
}
