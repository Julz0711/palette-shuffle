package com.julz.paletteshuffle.compat;
import com.julz.paletteshuffle.client.GalleryScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
public final class ModMenuIntegration implements ModMenuApi {
    @Override public ConfigScreenFactory<?> getModConfigScreenFactory() { return GalleryScreen::new; }
}
