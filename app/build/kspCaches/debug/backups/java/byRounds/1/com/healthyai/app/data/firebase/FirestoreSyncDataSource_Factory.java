package com.healthyai.app.data.firebase;

import com.google.firebase.firestore.FirebaseFirestore;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class FirestoreSyncDataSource_Factory implements Factory<FirestoreSyncDataSource> {
  private final Provider<FirebaseFirestore> firestoreProvider;

  public FirestoreSyncDataSource_Factory(Provider<FirebaseFirestore> firestoreProvider) {
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public FirestoreSyncDataSource get() {
    return newInstance(firestoreProvider.get());
  }

  public static FirestoreSyncDataSource_Factory create(
      Provider<FirebaseFirestore> firestoreProvider) {
    return new FirestoreSyncDataSource_Factory(firestoreProvider);
  }

  public static FirestoreSyncDataSource newInstance(FirebaseFirestore firestore) {
    return new FirestoreSyncDataSource(firestore);
  }
}
