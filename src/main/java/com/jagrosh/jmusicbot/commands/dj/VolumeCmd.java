/*
 * Copyright 2016 John Grosh <john.a.grosh@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.jmusicbot.commands.dj;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.commands.DJCommand;
import com.jagrosh.jmusicbot.settings.Settings;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Collections;

/**
 *
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class VolumeCmd extends DJCommand {

  public VolumeCmd(Bot bot) {
    super(bot);
    this.name = "volume";
    this.aliases = bot.getConfig().getAliases(this.name);
    this.help = "Устанавливает громкость пластинок";
    this.arguments = "[0-500]";
    this.options = Collections.singletonList(new OptionData(OptionType.INTEGER, "volume", "Громкость бота (От 0 до 500).").setRequired(false).setRequiredRange(0, 500));
  }

  @Override
  public void doCommand(CommandEvent event) {
    AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
    Settings settings = event.getClient().getSettingsFor(event.getGuild());
    int volume = handler.getPlayer().getVolume();
    if (event.getArgs().isEmpty()) {
      event.reply(FormatUtil.volumeIcon(volume) + " В данный момент громкость равна `" + volume + "`");
    } else {
      int nvolume;
      try {
        nvolume = Integer.parseInt(event.getArgs());
      } catch (NumberFormatException e) {
        nvolume = -1;
      }
      if (nvolume < 0 || nvolume > 500)
        event.reply(event.getClient().getError() + " Громкость должна быть целым числом между 0 и 500!");
      else {
        handler.getPlayer().setVolume(nvolume);
        settings.setVolume(nvolume);
        event.reply(FormatUtil.volumeIcon(nvolume) + " Громкость изменена из `" + volume + "` до `" + nvolume + "`");
      }
    }
  }

  @Override
  public void doSlashCommand(SlashCommandEvent event) {
    AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
    Settings settings = event.getClient().getSettingsFor(event.getGuild());
    int volume = handler.getPlayer().getVolume();

    if (!event.hasOption("volume")) {
      event.getHook().editOriginal(FormatUtil.volumeIcon(volume) + " В данный момент громкость равна `" + volume + "`")
        .queue();
    } else {
      int nvolume = event.getOption("volume").getAsInt();
      handler.getPlayer().setVolume(nvolume);
      settings.setVolume(nvolume);
      event.getHook().editOriginal(FormatUtil.volumeIcon(nvolume) + " Громкость изменена из `" + volume + "` до `" + nvolume + "`")
        .queue();
    }
  }
}
