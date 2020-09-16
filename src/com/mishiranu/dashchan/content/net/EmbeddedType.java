package com.mishiranu.dashchan.content.net;

import android.net.Uri;
import chan.content.ChanLocator;
import chan.content.model.EmbeddedAttachment;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
public enum EmbeddedType {
	YOUTUBE(Pattern.compile("(?:https?://)(?:www\\.)?(?:m\\.)?" +
			"youtu(?:\\.be/|be\\.com/(?:v/|embed/|(?:#/)?watch\\?(?:.*?|)v=))([\\w\\-]{11})"),
			1, Arrays.asList("youtu"), (locator, embeddedCode) -> {
		Uri fileUri = locator.buildQueryWithSchemeHost(true,
				"www.youtube.com", "watch", "v", embeddedCode);
		Uri thumbnailUri = locator.buildPathWithSchemeHost(true,
				"img.youtube.com", "vi", embeddedCode, "default.jpg");
		return new EmbeddedAttachment(fileUri, thumbnailUri, "YouTube",
				EmbeddedAttachment.ContentType.VIDEO, false, null);
	}),
	VIMEO(Pattern.compile("(?:https?://)(?:player\\.)?vimeo.com/(?:video/)?(?:channels/staffpicks/)?(\\d+)"),
			1, Arrays.asList("vimeo"), (locator, embeddedCode) -> {
		Uri fileUri = locator.buildPathWithSchemeHost(true, "vimeo.com", embeddedCode);
		return new EmbeddedAttachment(fileUri, null, "Vimeo",
				EmbeddedAttachment.ContentType.VIDEO, false, null);
	}),
	VOCAROO(Pattern.compile("(?:https?://)(?:www\\.)?" +
			"(?:media\\.vocaroo\\.com|vocaroo\\.com|voca.ro)/(?:player\\.swf\\?playMediaID=|i/|" +
			"media_command\\.php\\?media=|mp3/|)([\\w\\-]{11,12})"),
			1, Arrays.asList("vocaroo", "voca.ro"), (locator, embeddedCode) -> {
		Uri fileUri = locator.buildPathWithHost("media.vocaroo.com", "mp3", embeddedCode);
		String forcedName = "Vocaroo_" + embeddedCode + ".mp3";
		return new EmbeddedAttachment(fileUri, null, "Vocaroo",
				EmbeddedAttachment.ContentType.AUDIO, true, forcedName);
	});

	private interface AttachmentBuilder {
		EmbeddedAttachment obtain(ChanLocator locator, String embeddedCode);
	}

	private final Pattern pattern;
	private final int index;
	private final List<String> test;
	private final AttachmentBuilder builder;

	EmbeddedType(Pattern pattern, int index, List<String> test, AttachmentBuilder builder) {
		this.pattern = pattern;
		this.index = index;
		this.test = test;
		this.builder = builder;
	}

	private boolean test(String text) {
		if (text == null) {
			return false;
		}
		boolean success = false;
		for (String test : this.test) {
			if (text.contains(test)) {
				success = true;
				break;
			}
		}
		return success;
	}

	public String get(ChanLocator locator, String text) {
		return test(text) ? locator.getGroupValue(text, pattern, index) : null;
	}

	public String[] getAll(ChanLocator locator, String text) {
		return test(text) ? locator.getUniqueGroupValues(text, pattern, index) : null;
	}

	public EmbeddedAttachment obtainAttachment(ChanLocator locator, String embeddedCode) {
		return builder.obtain(locator, embeddedCode);
	}

	public static EmbeddedAttachment extractAttachment(String data) {
		if (data != null) {
			ChanLocator locator = ChanLocator.getDefault();
			for (EmbeddedType embeddedType : EmbeddedType.values()) {
				String embeddedCode = embeddedType.get(locator, data);
				if (embeddedCode != null) {
					return embeddedType.obtainAttachment(locator, embeddedCode);
				}
			}
		}
		return null;
	}
}