package com.healthyai.app.presentation.screen.habits.addedit;

import androidx.lifecycle.SavedStateHandle;
import com.healthyai.app.domain.repository.HabitRepository;
import com.healthyai.app.domain.usecase.habit.AddHabitUseCase;
import com.healthyai.app.domain.usecase.habit.DeleteHabitUseCase;
import com.healthyai.app.domain.usecase.habit.UpdateHabitUseCase;
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
public final class AddEditHabitViewModel_Factory implements Factory<AddEditHabitViewModel> {
  private final Provider<AddHabitUseCase> addHabitUseCaseProvider;

  private final Provider<UpdateHabitUseCase> updateHabitUseCaseProvider;

  private final Provider<DeleteHabitUseCase> deleteHabitUseCaseProvider;

  private final Provider<HabitRepository> repositoryProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public AddEditHabitViewModel_Factory(Provider<AddHabitUseCase> addHabitUseCaseProvider,
      Provider<UpdateHabitUseCase> updateHabitUseCaseProvider,
      Provider<DeleteHabitUseCase> deleteHabitUseCaseProvider,
      Provider<HabitRepository> repositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.addHabitUseCaseProvider = addHabitUseCaseProvider;
    this.updateHabitUseCaseProvider = updateHabitUseCaseProvider;
    this.deleteHabitUseCaseProvider = deleteHabitUseCaseProvider;
    this.repositoryProvider = repositoryProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public AddEditHabitViewModel get() {
    return newInstance(addHabitUseCaseProvider.get(), updateHabitUseCaseProvider.get(), deleteHabitUseCaseProvider.get(), repositoryProvider.get(), savedStateHandleProvider.get());
  }

  public static AddEditHabitViewModel_Factory create(
      Provider<AddHabitUseCase> addHabitUseCaseProvider,
      Provider<UpdateHabitUseCase> updateHabitUseCaseProvider,
      Provider<DeleteHabitUseCase> deleteHabitUseCaseProvider,
      Provider<HabitRepository> repositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new AddEditHabitViewModel_Factory(addHabitUseCaseProvider, updateHabitUseCaseProvider, deleteHabitUseCaseProvider, repositoryProvider, savedStateHandleProvider);
  }

  public static AddEditHabitViewModel newInstance(AddHabitUseCase addHabitUseCase,
      UpdateHabitUseCase updateHabitUseCase, DeleteHabitUseCase deleteHabitUseCase,
      HabitRepository repository, SavedStateHandle savedStateHandle) {
    return new AddEditHabitViewModel(addHabitUseCase, updateHabitUseCase, deleteHabitUseCase, repository, savedStateHandle);
  }
}
