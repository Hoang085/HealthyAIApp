package com.healthyai.app.domain.usecase.habit;

import com.healthyai.app.domain.repository.HabitRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class UpdateHabitUseCase_Factory implements Factory<UpdateHabitUseCase> {
  private final Provider<HabitRepository> repositoryProvider;

  public UpdateHabitUseCase_Factory(Provider<HabitRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public UpdateHabitUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static UpdateHabitUseCase_Factory create(Provider<HabitRepository> repositoryProvider) {
    return new UpdateHabitUseCase_Factory(repositoryProvider);
  }

  public static UpdateHabitUseCase newInstance(HabitRepository repository) {
    return new UpdateHabitUseCase(repository);
  }
}
