## What is jEmoji?

**jEmoji** is a plugin which provides you a simple way to let your users type emojis in your input forms. A lot of social applications like Twitter, Slack or Facebook integrate them, and now there is no reason for you to do the same.

- [Official page](http://franverona.com/jemoji)
- [Video demo](https://youtu.be/ymhaXTRhGZ0) (Youtube Link)

## How to use jEmoji

**jEmoji** needs [jQuery](http://jquery.com/download/) to work, so you have to include [jQuery](http://jquery.com/download/) in your page **BEFORE** using this plugin. I tested it with jQuery v1.11.2, and it works well.

1. Download jEmoji from [this link](https://github.com/franverona/jemoji/archive/master.zip) or [clone it from GitHub](https://github.com/franverona/jemoji)

2. Uncompress it (if zipped) and copy **jEmoji** folder into your JS scripts.

3. Insert the following code in your webpage:

    <script type="text/javascript" src="jemoji/jemoji.js"></script>

You can also use the [minified](http://en.wikipedia.org/wiki/Minification_(programming)) version:

    <script type="text/javascript" src="jemoji/jemoji.min.js"></script>

## Examples

#### You can check for examples following this link: [http://franverona.com/jemoji/](http://franverona.com/jemoji/)

### Default

jEmoji will create emoji menu and it will start detection when user type ':' (colon). After two characters, emoji menu will display emoji matching user input.

    $('#example').jemoji();

### Open emoji menu when press button

Using [Bootstrap input groups](http://getbootstrap.com/components/#input-groups) we allow our users to open emoji menu on click an specific button.

    $('#example').jemoji({
      menuBtn:    $('#show-menu'),
      container:  $('#example').parent().parent()
    });

### Custom translation

jEmoji only supports english and spanish language, but you can specify your own translation for navigation info messages.

    $('#example').jemoji({
      language:     {
        arrow:    '&#8592; / &#8594; pour naviguer',
        select:   '&#8629; pour sélectionner',
        esc:      'esc pour fermer',
        close:    'Fermer'
      }
    });

### Themes

jEmoji includes a CSS file with some standard color schemes. You can use one of them, or create yours for a better customization.
  
    $('#example').jemoji({
      theme:    'red'
    });

### Custom icons

You can also use your own emoji images. In this example we used [Onion Head](http://emoticoner.com/emoticons/onion-head) images as emojis. We only need to provide an array of names (emoji file names), file extension and folder images path.

    $('#example').jemoji({
      icons:        ["admire2", "admire", "ahaaah", "angel1", "angel2", "bad_atmosphere", "beaten", "beg", "big_eye", "bike", "bird", "bled1", "bled2", "bleeding", "blocked", "bsod", "bye1", "bye2", "cheer1", "cheer2", "cheer3", "confused", "congrats", "cool", "cruch", "crying1", "crying2", "crying3", "cute2", "dead", "depressed1", "depressed2", "desperate1", "desperate2", "dong", "dreaming", "dying", "eaten_alive", "eating_me", "embarrassed1", "embarrassed2", "embarrassed3", "embarrassed4", "evil_smile", "expulsion", "falling_asleep", "freezing", "frozen", "full", "ghost", "good_job", "good_luck", "happy", "hate", "hehe", "hell_yes", "help", "hi", "hot1", "hot2", "hypnosis", "ill", "info", "innocent", "kicked1", "kicked2", "kick", "lie", "lol1", "lol2", "lonely", "love", "meh", "nonono", "noooo", "not_listening", "objection", "oh", "onionspayup", "pff1", "pff2", "pointing", "pretty", "punch", "push_up", "relax1", "relax2", "rice_ball_smiley_10", "robot", "running", "scared", "scary", "serenade", "shock1", "shock2", "shy", "sigh", "silence", "sleeping", "smoking1", "smoking2", "spa", "starving", "stoned", "stress", "studying", "super", "super_sayan", "sweating", "sweetdrop", "tar", "uhuhuh", "victory", "vomiting", "wait", "waiting", "warning", "washing", "wet", "wew", "whaaat1", "whaaat2", "whaaat3", "what", "whip", "whistling", "white_cloud_emoticon6", "woa", "work", "wow1", "wow2", "yawn"],
      extension:    'gif',
      folder:       'images/onions/'
    });

## Documentation

Before use jEmoji you need know that:

*   **jEmoji won't replace emoji names**. If you want to replace emoji name by its image, you have to develope your own code, or use some third-party plugins like [Emojify.js](https://github.com/Ranks/emojify.js) (see [Real Example](#real-example) subsection).
*   Emoji images should to be stored by yourself (locally or remote) and with read permissions.

### Options

| Name | Type | Default  | Description |
| --- | --- | --- | --- |
| icons | Array | [Apple emoji](http://emojipedia.org/apple/) array names. | Array of emoji names. |
| extension | String | 'png' | Emoji files extension. |
| folder | String | 'images/emojis/' | Emoji images folder (don't forget slash at the end!). |
| container | DOM | DOM parent input | DOM element where emoji menu should be appended. This option is useful when you have additional HTML with advanced features like [Bootstrap input groups](http://getbootstrap.com/components/#input-groups). |
| btn | DOM | null | DOM element for opening/closing emoji menu. |
| navigation | Boolean | true | True if you want to allow navigation keys (arrows/tab for choosing, enter to select and esc to dismiss), false otherwise. |
| language | JSON | String | 'en' | Translation for navigation info. Language JSON object currently have four properties: 'arrow', 'select', 'esc' and 'close'. |
| theme | String | 'blue' | Plugin theme and colour scheme. Possible values are 'blue', 'red', 'green' or 'black'. You can also create your own style by modifying jEmoji CSS file. See **Customization** section. |
| resize | Function | null | Resize function. jEmoji provides a function which automatically resizes emoji menu by adapting its width to input width, but you can use your own. |

### Methods

**Set options**: get/set jEmoji options.

    // return current jemoji options
    $('#example').jemoji('options');

    // set 'theme' option to red
    $('#example').jemoji('options', { 'theme': 'red' });

**Is open**: return true if emoji menu is open; false otherwise.

    $('#example').jemoji('isopen');

**Open emoji menu**: display emoji menu.

    $('#example').jemoji('open');

**Close emoji menu**: hide emoji menu.

    $('#example').jemoji('close');

### Real example

You have a "social network" and want your users to use emojis on their status, so you will need an emoji selector (like jEmoji) and an emoji translator (like [Emojify.js](https://github.com/Ranks/emojify.js)).

Javascript:

    // Set jEmoji
    $('#example').jemoji();

    // Run emojify on each addition
    $('#example').keypress(function (event) {
      if (event.which === 13) {
        var container = document.getElementById('example-container');
        container.innerHTML += this.value;
        emojify.run(container);             // translate emoji to images
        this.value = '';
        container.scrollTop = container.scrollHeight;
      }
    });

HTML:

    <div style="margin-top:10px;">
      <div id="real-example-container"></div>
      <input id="real-example" type="text" />
    </div>

**Important**: in this real example we used Emojify.js plugin. If you want to use your own emoji images with jEmoji, Emojify.js default functionality will not work, so you will have to develop your own replacer.

## Customization

**jEmoji** uses CSS to style emoji menu and includes four standard themes with a couple of colors. But you can create your own jEmoji styles to customize your emoji menu for you own page.

For example, **black** theme is coded like this:

    .jemoji-menu.black .jemoji-icons {
      border-color: #4e4e4e;
    }

    .jemoji-menu.black .jemoji-info {
      color: #d2d2d2;
      border-color: #4e4e4e;
      background-color: #4e4e4e;
    }

    .jemoji-menu.black .jemoji-icons > div:hover,
    .jemoji-menu.black .jemoji-icons > div:focus,
    .jemoji-menu.black .jemoji-icons > div:active,
    .jemoji-menu.black .jemoji-icons > div.active {
      background-color: #4e4e4e;
    }

    .jemoji-menu.black .jemoji-menu-arrow.up {
      border-bottom-color: #4e4e4e;
    }
             
By modifying colors, borders, font sizes and dimensions you can create a custom jEmoji theme in a few minutes.

---

**jEmoji** is under MIT License. Feel free to download, modify and adapt it to your own purposes. If you find any bug, send a pull request or write an issue.
