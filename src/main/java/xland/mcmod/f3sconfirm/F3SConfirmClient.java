package xland.mcmod.f3sconfirm;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.platform.TextureUtil;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.util.InputUtil;
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

        // Install a GLFW key callback on first client tick to intercept F3+S before Minecraft handles it.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            try {
                if (client == null || client.getWindow() == null) return;
                long handle = client.getWindow().getHandle();
                // ensure we only set the callback once
                if (installed.getAndSet(true)) return;

                final java.util.concurrent.atomic.AtomicReference<GLFWKeyCallbackI> prevRef = new java.util.concurrent.atomic.AtomicReference<>();
                GLFWKeyCallbackI callback = (window, key, scancode, action, mods) -> {
                    // If S pressed and F3 is currently down, intercept and show confirmation.
                    if (key == GLFW.GLFW_KEY_S && action == GLFW.GLFW_PRESS) {
                        int f3State = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_F3);
                        if (f3State == GLFW.GLFW_PRESS) {
                            F3SConfirmClient.debugError(F3SConfirmClient.getTerminationText());
                            return; // swallow event (don't call previous callback)
                        }
                    }
                    // forward to previous callback to preserve default behavior
                    GLFWKeyCallbackI previous = prevRef.get();
                    if (previous != null) previous.invoke(window, key, scancode, action, mods);
                };
                GLFWKeyCallbackI previous = GLFW.glfwSetKeyCallback(handle, callback);
                prevRef.set(previous);
            } catch (Throwable ignored) {
            }
        });
    }

    public static void execute(MinecraftClient client, Consumer<? super Text> feedbackSender) {
        Path path = TextureUtil.getDebugTexturePath(client.runDirectory.toPath()).toAbsolutePath();
        client.getTextureManager().dumpDynamicTextures(path);
        Text textInside = Text.literal(path.toString()).formatted(Formatting.UNDERLINE);
        feedbackSender.accept(Text.translatable("debug.dump_dynamic_textures", textInside));
    }

    public static Text getTerminationText() {
        return Text.translatableWithFallback("f3sconfirm.terminate",
                "Dumping dynamic textures with F3+S is disabled.\nType `/f3sconfirm` to confirm dumping.",
                Text.literal("/f3sconfirm").formatted(Formatting.UNDERLINE)
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

        private static final java.util.concurrent.atomic.AtomicBoolean installed = new java.util.concurrent.atomic.AtomicBoolean(false);

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
