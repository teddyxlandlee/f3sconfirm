package xland.mcmod.f3sconfirm;

import com.mojang.blaze3d.platform.TextureUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.nio.file.Path;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class F3SConfirmClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register(new Identifier("f3sconfirm", "default"), (dispatcher, registryAccess) -> {
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
}
