/**
 * Optional JSON {@link gg.cubix.adventurei18n.LangFileFormat} add-on for consumers who want JSON
 * lang files instead of {@code adventure-i18n-core}'s default {@code .properties} format.
 *
 * <p>This is the only class this artifact provides - it exists as a separate module specifically
 * so that depending on {@code adventure-i18n-core} alone never pulls in Gson, not even
 * transitively (see ADR-0001 in {@code adventure-i18n-core}'s repository).
 */
package gg.cubix.adventurei18n.json;
