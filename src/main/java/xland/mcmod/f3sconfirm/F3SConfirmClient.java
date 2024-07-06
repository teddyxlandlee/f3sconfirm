package xland.mcmod.f3sconfirm;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.platform.TextureUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.lang.invoke.*;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class F3SConfirmClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register(ofId("f3sconfirm", "default"), (dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("f3sconfirm")
                    .executes(context -> {
                        final FabricClientCommandSource commandSource = context.getSource();
                        execute(commandSource.getClient(), commandSource::sendFeedback);
                        return 1;
                    })
            );
        });
    }

    public static void execute(MinecraftClient client, Consumer<? super Text> feedbackSender) {
        Path path = TextureUtil.getDebugTexturePath(client.runDirectory.toPath()).toAbsolutePath();
        client.getTextureManager().dumpDynamicTextures(path);
        Text textInside = Text.literal(path.toString()).formatted(Formatting.UNDERLINE)
                .styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, path.toString())));
        feedbackSender.accept(Text.translatable("debug.dump_dynamic_textures", textInside));
    }

    public static Text getTerminationText() {
        return Text.translatableWithFallback("f3sconfirm.terminate",
                "Dumping dynamic textures with F3+S is disabled.\nType `/f3sconfirm` to confirm dumping.",
                Text.literal("/f3sconfirm")
                        .formatted(Formatting.UNDERLINE)
                        .styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/f3sconfirm")))
        ).formatted(Formatting.RED);
    }

    public static void debugError(Text text) {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.empty()
                .append(Text.translatable("debug.prefix")
                        .formatted(Formatting.RED, Formatting.BOLD))
                .append(ScreenTexts.SPACE)
                .append(text)
        );
    }

    private static final Supplier<MethodHandle> OF_IDENTIDIER = Suppliers.memoize(() -> {
    	var lookup = MethodHandles.lookup();
    	var methodType = MethodType.methodType(Identifier.class, String.class, String.class);
    	
    	boolean useIdentifierOf;
    	try {
    	    var mcVersion = FabricLoader.getInstance().getModContainer("minecraft").orElseThrow().getMetadata().getVersion();
    	    useIdentifierOf = VersionPredicate.parse(">=1.21-alpha.24.21.a").test(mcVersion);
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}

    	if (!useIdentifierOf) {
    	    try {
    	        return lookup.findConstructor(Identifier.class, methodType.changeReturnType(void.class));
    	    } catch (Exception e) {
    	    	throw new RuntimeException(e);
    	    }
    	}

    	String methodName = FabricLoader.getInstance().getMappingResolver().mapMethodName(
    	    "intermediary",
    		"net.minecraft.class_2960",
    		"method_60655",
    		"(Ljava/lang/String;Ljava/lang/String;)Lnet/minecraft/class_2960;"
    	);
    	try {
    	    return lookup.findStatic(Identifier.class, methodName, methodType);
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    });

    public static Identifier ofId(String namespace, String path) {
        try {
    	    return (Identifier) OF_IDENTIDIER.get().invokeExact(namespace, path);
    	} catch (Throwable t) {
    		throw new RuntimeException(t);
    	}
    }

}
