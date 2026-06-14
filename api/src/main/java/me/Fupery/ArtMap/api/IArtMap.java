package me.Fupery.ArtMap.api;

import java.io.File;
import java.io.Reader;

import org.bukkit.plugin.Plugin;

import me.Fupery.ArtMap.api.Painting.IArtistHandler;

public interface IArtMap extends Plugin {

	public IArtistHandler getArtistHandler();
	public Reader getTextResourceFile(String fileName);
	public boolean writeResource(String resourcePath, File destination);

}
