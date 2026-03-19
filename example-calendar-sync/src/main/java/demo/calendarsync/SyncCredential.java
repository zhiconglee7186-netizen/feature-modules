package demo.calendarsync;

public record SyncCredential(
        long userId,
        ProviderKind providerKind,
        String accountRef,
        String accessToken,
        String refreshToken,
        String encryptedSecret
) {
}
