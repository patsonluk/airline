if (typeof(jQuery) === 'undefined') { 
  throw new Error('jEmoji requires jQuery');
}

(function($) {

  var jemoji = function ($el, options) {

    var _this = this;

    this._icons = [
      '+1', '-1', '100', '1234', '8ball', 'a', 'ab', 'abc', 'abcd', 'accept', 'aerial_tramway', 'airplane', 'alarm_clock', 'alien', 'ambulance', 'anchor', 'angel', 'anger', 'angry', 'anguished', 'ant', 'apple', 'aquarius', 'aries', 'arrow_backward', 'arrow_double_down', 'arrow_double_up', 'arrow_down', 'arrow_down_small', 'arrow_forward', 'arrow_heading_down', 'arrow_heading_up', 'arrow_left', 'arrow_lower_left', 'arrow_lower_right', 'arrow_right', 'arrow_right_hook', 'arrow_up', 'arrow_up_down', 'arrow_up_small', 'arrow_upper_left', 'arrow_upper_right', 'arrows_clockwise', 'arrows_counterclockwise', 'art', 'articulated_lorry', 'astonished', 'atm', 'b', 'baby', 'baby_bottle', 'baby_chick', 'baby_symbol', 'back', 'baggage_claim', 'balloon', 'ballot_box_with_check', 'bamboo', 'banana', 'bangbang', 'bank', 'bar_chart', 'barber', 'baseball', 'basketball', 'bath', 'bathtub', 'battery', 'bear', 'bee', 'beer', 'beers', 'beetle', 'beginner', 'bell', 'bento', 'bicyclist', 'bike', 'bikini', 'bird', 'birthday', 'black_circle', 'black_joker', 'black_medium_small_square', 'black_medium_square', 'black_nib', 'black_small_square', 'black_square', 'black_square_button', 'blossom', 'blowfish', 'blue_book', 'blue_car', 'blue_heart', 'blush', 'boar', 'boat', 'bomb', 'book', 'bookmark', 'bookmark_tabs', 'books', 'boom', 'boot', 'bouquet', 'bow', 'bowling', 'bowtie', 'boy', 'bread', 'bride_with_veil', 'bridge_at_night', 'briefcase', 'broken_heart', 'bug', 'bulb', 'bullettrain_front', 'bullettrain_side', 'bus', 'busstop', 'bust_in_silhouette', 'busts_in_silhouette', 'cactus', 'cake', 'calendar', 'calling', 'camel', 'camera', 'cancer', 'candy', 'capital_abcd', 'capricorn', 'car', 'card_index', 'carousel_horse', 'cat', 'cat2', 'cd', 'chart', 'chart_with_downwards_trend', 'chart_with_upwards_trend', 'checkered_flag', 'cherries', 'cherry_blossom', 'chestnut', 'chicken', 'children_crossing', 'chocolate_bar', 'christmas_tree', 'church', 'cinema', 'circus_tent', 'city_sunrise', 'city_sunset', 'cl', 'clap', 'clapper', 'clipboard', 'clock1', 'clock10', 'clock1030', 'clock11', 'clock1130', 'clock12', 'clock1230', 'clock130', 'clock2', 'clock230', 'clock3', 'clock330', 'clock4', 'clock430', 'clock5', 'clock530', 'clock6', 'clock630', 'clock7', 'clock730', 'clock8', 'clock830', 'clock9', 'clock930', 'closed_book', 'closed_lock_with_key', 'closed_umbrella', 'cloud', 'clubs', 'cn', 'cocktail', 'coffee', 'cold_sweat', 'collision', 'computer', 'confetti_ball', 'confounded', 'confused', 'congratulations', 'construction', 'construction_worker', 'convenience_store', 'cookie', 'cool', 'cop', 'copyright', 'corn', 'couple', 'couple_with_heart', 'couplekiss', 'cow', 'cow2', 'credit_card', 'crescent_moon', 'crocodile', 'crossed_flags', 'crown', 'cry', 'crying_cat_face', 'crystal_ball', 'cupid', 'curly_loop', 'currency_exchange', 'curry', 'custard', 'customs', 'cyclone', 'dancer', 'dancers', 'dango', 'dart', 'dash', 'date', 'de', 'deciduous_tree', 'department_store', 'diamond_shape_with_a_dot_inside', 'diamonds', 'disappointed', 'disappointed_relieved', 'dizzy', 'dizzy_face', 'do_not_litter', 'dog', 'dog2', 'dollar', 'dolls', 'dolphin', 'donut', 'door', 'doughnut', 'dragon', 'dragon_face', 'dress', 'dromedary_camel', 'droplet', 'dvd', 'e-mail', 'ear', 'ear_of_rice', 'earth_africa', 'earth_americas', 'earth_asia', 'egg', 'eggplant', 'eight', 'eight_pointed_black_star', 'eight_spoked_asterisk', 'electric_plug', 'elephant', 'email', 'end', 'envelope', 'es', 'euro', 'european_castle', 'european_post_office', 'evergreen_tree', 'exclamation', 'expressionless', 'eyeglasses', 'eyes', 'facepunch', 'factory', 'fallen_leaf', 'family', 'fast_forward', 'fax', 'fearful', 'feelsgood', 'feet', 'ferris_wheel', 'file_folder', 'finnadie', 'fire', 'fire_engine', 'fireworks', 'first_quarter_moon', 'first_quarter_moon_with_face', 'fish', 'fish_cake', 'fishing_pole_and_fish', 'fist', 'five', 'flags', 'flashlight', 'floppy_disk', 'flower_playing_cards', 'flushed', 'foggy', 'football', 'fork_and_knife', 'fountain', 'four', 'four_leaf_clover', 'fr', 'free', 'fried_shrimp', 'fries', 'frog', 'frowning', 'fu', 'fuelpump', 'full_moon', 'full_moon_with_face', 'game_die', 'gb', 'gem', 'gemini', 'ghost', 'gift', 'gift_heart', 'girl', 'globe_with_meridians', 'goat', 'goberserk', 'godmode', 'golf', 'grapes', 'green_apple', 'green_book', 'green_heart', 'grey_exclamation', 'grey_question', 'grimacing', 'grin', 'grinning', 'guardsman', 'guitar', 'gun', 'haircut', 'hamburger', 'hammer', 'hamster', 'hand', 'handbag', 'hankey', 'hash', 'hatched_chick', 'hatching_chick', 'headphones', 'hear_no_evil', 'heart', 'heart_decoration', 'heart_eyes', 'heart_eyes_cat', 'heartbeat', 'heartpulse', 'hearts', 'heavy_check_mark', 'heavy_division_sign', 'heavy_dollar_sign', 'heavy_exclamation_mark', 'heavy_minus_sign', 'heavy_multiplication_x', 'heavy_plus_sign', 'helicopter', 'herb', 'hibiscus', 'high_brightness', 'high_heel', 'hocho', 'honey_pot', 'honeybee', 'horse', 'horse_racing', 'hospital', 'hotel', 'hotsprings', 'hourglass', 'hourglass_flowing_sand', 'house', 'house_with_garden', 'hurtrealbad', 'hushed', 'ice_cream', 'icecream', 'id', 'ideograph_advantage', 'imp', 'inbox_tray', 'incoming_envelope', 'information_desk_person', 'information_source', 'innocent', 'interrobang', 'iphone', 'it', 'izakaya_lantern', 'jack_o_lantern', 'japan', 'japanese_castle', 'japanese_goblin', 'japanese_ogre', 'jeans', 'joy', 'joy_cat', 'jp', 'key', 'keycap_ten', 'kimono', 'kiss', 'kissing', 'kissing_cat', 'kissing_closed_eyes', 'kissing_face', 'kissing_heart', 'kissing_smiling_eyes', 'koala', 'koko', 'kr', 'large_blue_circle', 'large_blue_diamond', 'large_orange_diamond', 'last_quarter_moon', 'last_quarter_moon_with_face', 'laughing', 'leaves', 'ledger', 'left_luggage', 'left_right_arrow', 'leftwards_arrow_with_hook', 'lemon', 'leo', 'leopard', 'libra', 'light_rail', 'link', 'lips', 'lipstick', 'lock', 'lock_with_ink_pen', 'lollipop', 'loop', 'loudspeaker', 'love_hotel', 'love_letter', 'low_brightness', 'm', 'mag', 'mag_right', 'mahjong', 'mailbox', 'mailbox_closed', 'mailbox_with_mail', 'mailbox_with_no_mail', 'man', 'man_with_gua_pi_mao', 'man_with_turban', 'mans_shoe', 'maple_leaf', 'mask', 'massage', 'meat_on_bone', 'mega', 'melon', 'memo', 'mens', 'metal', 'metro', 'microphone', 'microscope', 'milky_way', 'minibus', 'minidisc', 'mobile_phone_off', 'money_with_wings', 'moneybag', 'monkey', 'monkey_face', 'monorail', 'mortar_board', 'mount_fuji', 'mountain_bicyclist', 'mountain_cableway', 'mountain_railway', 'mouse', 'mouse2', 'movie_camera', 'moyai', 'muscle', 'mushroom', 'musical_keyboard', 'musical_note', 'musical_score', 'mute', 'nail_care', 'name_badge', 'neckbeard', 'necktie', 'negative_squared_cross_mark', 'neutral_face', 'new', 'new_moon', 'new_moon_with_face', 'newspaper', 'ng', 'nine', 'no_bell', 'no_bicycles', 'no_entry', 'no_entry_sign', 'no_good', 'no_mobile_phones', 'no_mouth', 'no_pedestrians', 'no_smoking', 'non-potable_water', 'nose', 'notebook', 'notebook_with_decorative_cover', 'notes', 'nut_and_bolt', 'o', 'o2', 'ocean', 'octocat', 'octopus', 'oden', 'office', 'ok', 'ok_hand', 'ok_woman', 'older_man', 'older_woman', 'on', 'oncoming_automobile', 'oncoming_bus', 'oncoming_police_car', 'oncoming_taxi', 'one', 'open_file_folder', 'open_hands', 'open_mouth', 'ophiuchus', 'orange_book', 'outbox_tray', 'ox', 'package', 'page_facing_up', 'page_with_curl', 'pager', 'palm_tree', 'panda_face', 'paperclip', 'parking', 'part_alternation_mark', 'partly_sunny', 'passport_control', 'paw_prints', 'peach', 'pear', 'pencil', 'pencil2', 'penguin', 'pensive', 'performing_arts', 'persevere', 'person_frowning', 'person_with_blond_hair', 'person_with_pouting_face', 'phone', 'pig', 'pig2', 'pig_nose', 'pill', 'pineapple', 'pisces', 'pizza', 'plus1', 'point_down', 'point_left', 'point_right', 'point_up', 'point_up_2', 'police_car', 'poodle', 'poop', 'post_office', 'postal_horn', 'postbox', 'potable_water', 'pouch', 'poultry_leg', 'pound', 'pouting_cat', 'pray', 'princess', 'punch', 'purple_heart', 'purse', 'pushpin', 'put_litter_in_its_place', 'question', 'rabbit', 'rabbit2', 'racehorse', 'radio', 'radio_button', 'rage', 'rage1', 'rage2', 'rage3', 'rage4', 'railway_car', 'rainbow', 'raised_hand', 'raised_hands', 'raising_hand', 'ram', 'ramen', 'rat', 'recycle', 'red_car', 'red_circle', 'registered', 'relaxed', 'relieved', 'repeat', 'repeat_one', 'restroom', 'revolving_hearts', 'rewind', 'ribbon', 'rice', 'rice_ball', 'rice_cracker', 'rice_scene', 'ring', 'rocket', 'roller_coaster', 'rooster', 'rose', 'rotating_light', 'round_pushpin', 'rowboat', 'ru', 'rugby_football', 'runner', 'running', 'running_shirt_with_sash', 'sa', 'sagittarius', 'sailboat', 'sake', 'sandal', 'santa', 'satellite', 'satisfied', 'saxophone', 'school', 'school_satchel', 'scissors', 'scorpius', 'scream', 'scream_cat', 'scroll', 'seat', 'secret', 'see_no_evil', 'seedling', 'seven', 'shaved_ice', 'sheep', 'shell', 'ship', 'shipit', 'shirt', 'shit', 'shoe', 'shower', 'signal_strength', 'six', 'six_pointed_star', 'ski', 'skull', 'sleeping', 'sleepy', 'slot_machine', 'small_blue_diamond', 'small_orange_diamond', 'small_red_triangle', 'small_red_triangle_down', 'smile', 'smile_cat', 'smiley', 'smiley_cat', 'smiling_imp', 'smirk', 'smirk_cat', 'smoking', 'snail', 'snake', 'snowboarder', 'snowflake', 'snowman', 'sob', 'soccer', 'soon', 'sos', 'sound', 'space_invader', 'spades', 'spaghetti', 'sparkle', 'sparkler', 'sparkles', 'sparkling_heart', 'speak_no_evil', 'speaker', 'speech_balloon', 'speedboat', 'squirrel', 'star', 'star2', 'stars', 'station', 'statue_of_liberty', 'steam_locomotive', 'stew', 'straight_ruler', 'strawberry', 'stuck_out_tongue', 'stuck_out_tongue_closed_eyes', 'stuck_out_tongue_winking_eye', 'sun_with_face', 'sunflower', 'sunglasses', 'sunny', 'sunrise', 'sunrise_over_mountains', 'surfer', 'sushi', 'suspect', 'suspension_railway', 'sweat', 'sweat_drops', 'sweat_smile', 'sweet_potato', 'swimmer', 'symbols', 'syringe', 'tada', 'tanabata_tree', 'tangerine', 'taurus', 'taxi', 'tea', 'telephone', 'telephone_receiver', 'telescope', 'tennis', 'tent', 'thought_balloon', 'three', 'thumbsdown', 'thumbsup', 'ticket', 'tiger', 'tiger2', 'tired_face', 'tm', 'toilet', 'tokyo_tower', 'tomato', 'tongue', 'top', 'tophat', 'tractor', 'traffic_light', 'train', 'train2', 'tram', 'triangular_flag_on_post', 'triangular_ruler', 'trident', 'triumph', 'trolleybus', 'trollface', 'trophy', 'tropical_drink', 'tropical_fish', 'truck', 'trumpet', 'tshirt', 'tulip', 'turtle', 'tv', 'twisted_rightwards_arrows', 'two', 'two_hearts', 'two_men_holding_hands', 'two_women_holding_hands', 'u5272', 'u5408', 'u55b6', 'u6307', 'u6708', 'u6709', 'u6e80', 'u7121', 'u7533', 'u7981', 'u7a7a', 'uk', 'umbrella', 'unamused', 'underage', 'unlock', 'up', 'us', 'v', 'vertical_traffic_light', 'vhs', 'vibration_mode', 'video_camera', 'video_game', 'violin', 'virgo', 'volcano', 'vs', 'walking', 'waning_crescent_moon', 'waning_gibbous_moon', 'warning', 'watch', 'water_buffalo', 'watermelon', 'wave', 'wavy_dash', 'waxing_crescent_moon', 'waxing_gibbous_moon', 'wc', 'weary', 'wedding', 'whale', 'whale2', 'wheelchair', 'white_check_mark', 'white_circle', 'white_flower', 'white_large_square', 'white_medium_small_square', 'white_medium_square', 'white_small_square', 'white_square_button', 'wind_chime', 'wine_glass', 'wink', 'wolf', 'woman', 'womans_clothes', 'womans_hat', 'womens', 'worried', 'wrench', 'x', 'yellow_heart', 'yen', 'yum', 'zap', 'zero', 'zzz'
    ];

    this._language = {
      'es':     {
        'arrow':    '&#8592; / &#8594; para navegar',
        'select':   '&#8629; para seleccionar',
        'esc':      'esc para cerrar',
        'close':    'Cerrar'
      },
      'en':     {
        'arrow':    '&#8592; / &#8594; to navigate',
        'select':   '&#8629; to select',
        'esc':      'esc to dismiss',
        'close':    'Close'
      }
    };

    // Default options
    this._defaults = {
      icons:          undefined,          // Custom icons. Default is undefined.
      extension:      'png',              // Icons type. Default is png.
      folder:         'images/emojis/',   // Emoji images folder. Default is 'images/emojis'.
      container:      undefined,          // Container to append menu. Default is 'undefined'.
      btn:            undefined,          // Dom element for opening emoji menu. Default is 'undefined'.
      navigation:     true,               // Navigation keys. Default is 'true'.
      language:       'en',               // Info messages language. Default is 'en'.
      theme:          'blue',             // Style theme. Default is 'blue'.
      resize:         undefined           // Resize function. Default is undefined.
    };

    this._options = $.extend(true, {}, this._defaults, options);

    // Custom icons
    if (typeof(this._options.icons) !== 'undefined') {
      this._icons = this._options.icons;
    }

    /**
     * Get/Set plugin options
     * @param  {json} options Plugin options
     * @return {json}         Current plugin options
     */
    this.options = function (options) {
      return (options) ? $.extend(true, this._options, options) : this._options;
    };

    /**
     * Return true if emoji menu is visible; false otherwise
     * @return {Boolean} Emoji menu visibility
     */
    this.isOpen = function () {
      return $(menuContainer).is(':visible');
    };

    var iconMenuOpenOnClick

    /**
     * Open emoji menu.
     */
    this.open = function (openOnClick) { //whether this menu is opened by clicking on the smiley or from :filtering
      iconMenuOpenOnClick = openOnClick
      $(menuContainer).show();
      selectedEmoji = false; //have to reset it since when the menu first open, the first selected item does not replace the current input
      var $icons = $(menuContainer).find('.jemoji-icons');

      if ($icons.html() === '') {

        var dir = this._defaults.folder;
        if (this._options.folder) {
          dir = this._options.folder;
        }

        var dom = $icons[0], innerHTML = '', ext = (typeof(this._options.extension))? this._options.extension : this._defaults;
        for (var i = 0, l = 100; i < l; i++) {
          var element = _this._icons[i], classActive = '';
          if (innerHTML === '')
            classActive = 'class="active"';   // First emoji set as 'active'

          innerHTML += '<div ' + classActive + '>' + 
                          '<img src="' + dir + element + '.' + ext + '" alt="' + element + '" />' +
                          '<span>:' + element + ':</span>' +
                        '</div>';
        }
        dom.innerHTML = innerHTML;

        $el.data('jemojiclick').call();

        var index = 0;
        $icons.on('scroll', function () {
          if($(this).scrollTop() + $(this).innerHeight() >= $(this)[0].scrollHeight) {
            if (100 * (++index) < _this._icons.length) {
              var innerHTML = '';
              for (var i = 100 * index, l = 100 + 100 * index; i < l; i++) {
                var element = _this._icons[i];
                if (element) {
                  innerHTML += '<div>' + 
                                      '<img src="' + dir + element + '.' + ext + '" alt="' + element + '" />' +
                                      '<span>:' + element + ':</span>' +
                                    '</div>';
                }
              }
              dom.innerHTML += innerHTML;
              $el.data('jemojiclick').call();
            }
          }
        });
      }

      var selectedDiv = $(".jemoji-icons .active") //on open always scroll to the selected icon
      if (selectedDiv) {
          selectedDiv[0].scrollIntoView();
          selectedDiv.parent()[0].scrollTop -= 10;
      }
    };

    /**
     * Close emoji menu
     */
    this.close = function () {
      $(menuContainer).hide();
    };

    // Get current cursor position
    var getCursorPosition = function () {

      if ($el.length === 0) {
        return 0;
      }

      var input = $el[0], pos = input.value.length;

      try {
        if (input.createTextRange) {
          var r = document.selection.createRange().duplicate();
          r.moveEnd('character', input.value.length);
          if (r.text === '') {
            pos = input.value.length;
          }
          pos = input.value.lastIndexOf(r.text);
        }
        else {
          if (typeof(input.selectionStart) !== 'undefined') {
            pos = input.selectionStart;
          }
        }
      }
      catch (e) {
        // IE bug with createTextRange
      }

      return pos;
    };

    //
    // Detect key events on mobile devices
    // http://stackoverflow.com/a/20508727/552669
    //
    function newKeyUpDown(originalFunction, eventType) {
      return function() {
        if ("ontouchstart" in document.documentElement) {
          var $element = $(this),
            $input = null;
          if (/input/i.test($element.prop('tagName')))
            $input = $element;
          else if ($('input', $element).size() > 0)
            $input = $($('input', $element).get(0));

          if ($input) {
            var currentVal = $input.val(),
              checkInterval = null;
            $input.focus(function(e) {
              clearInterval(checkInterval);
              checkInterval = setInterval(function() {
                if ($input.val() != currentVal) {
                  var event = jQuery.Event(eventType);
                  currentVal = $input.val();
                  event.which = event.keyCode = (currentVal && currentVal.length > 0) ? currentVal.charCodeAt(currentVal.length - 1) : '';
                  $input.trigger(event);
                }
              }, 30);
            });
            $input.blur(function() {
              clearInterval(checkInterval);
            });
          }
        }
        return originalFunction.apply(this, arguments);
      }
    }
    $.fn.jemojiKeyup = newKeyUpDown($.fn.keyup, 'keyup');
    $.fn.jemojiKeydown = newKeyUpDown($.fn.keydown, 'keydown');
    $.fn.jemojiKeypress = newKeyUpDown($.fn.keypress, 'keypress');

    $el.data('jemojiclick', function () { //openOnClick whether the menu was brought up my clicking the smiley icon. As the string replacement handling should be different
      $(d).find('div').off('click').on('click', function () {
        var emojiCode = $(this).find('img').attr('alt'), cursor = getCursorPosition(), value = $el.val();
        if (iconMenuOpenOnClick) { //simply insert :emojiCode:<space> at current cursor position
            value = value.slice(0, cursor) + ':' + emojiCode + ': ' + value.slice(cursor);
        } else {
            value = value.slice(0, value.lastIndexOf(':', cursor)) + ':' + emojiCode + ': ' + value.slice(cursor);
        }
        $el.val(value);
        $el.focus();
        menuOpened = false;
        currentChars = '';
        _this.close();
      });
    });

    // Emoji menu container
    var menuContainer = document.createElement('div'), $menuContainer = $(menuContainer);
    if (this._options.theme)
      menuContainer.className = 'jemoji-menu ' + this._options.theme;
    else
      menuContainer.className = 'jemoji-menu ' + this._defaults.theme;

    // Emoji icons container
    var d = document.createElement('div');
    d.className = 'jemoji-icons';
    $el.after($(d));

    // Arrow (pure css)
    var arrow = document.createElement('div');
    arrow.className = 'jemoji-menu-arrow up';
    $(d).before($(arrow));

    $menuContainer.append($(d));

    // Navigation info
    var dinfo = document.createElement('div');
    dinfo.className = 'jemoji-info';

    // Translation
    var arr = this._language[this._options.language];
    if (typeof(arr) === 'undefined') {
      // Custom translation
      arr = this._language[this._defaults.language];
    }

    var hasnavigation = (typeof(this._options.navigation) !== 'undefined')? this._options.navigation : this._defaults.navigation;
    if (hasnavigation) {
      arr = jQuery.map(arr, function (el) { return el; });
      dinfo.innerHTML = '';
      for (var i = 0, l = arr.length - 1; i < l; i++) {
        dinfo.innerHTML += '<div>' + arr[i] + '</div>';
      }
      dinfo.innerHTML += '<div class="jemoji-close">' + arr[arr.length - 1] + '</div>';
    }
    else {
      dinfo.innerHTML = '<div class="jemoji-close">' + arr.close + '</div>';
    }
    $menuContainer.prepend($(dinfo));
    $(dinfo).css('width', $el.css('width'));

    // Close emoji menu on click 'Close' button
    $(dinfo).find('.jemoji-close').on('click', function () {
      _this.close();
    });

    $menuContainer.prepend($(arrow));

    // Adapt emoji menu width to fit input width
    $menuContainer.css('width', $el.css('width'));
    $(d).css('width', $el.css('width'));

    // Append to container
    var appendContainer = (typeof(this._options.container) !== 'undefined')? this._options.container : this._defaults.container;
    if (appendContainer) {
      if (!(appendContainer instanceof jQuery)) 
        appendContainer = $(appendContainer);
      appendContainer.append($menuContainer);
    }
    else {
      $el.after($menuContainer);
    }

    var menuOpened = false, arrowsCursorBegin, arrowsCursorEnd, selectedEmoji = false;

    // Trigger open/close emoji menu
    var btn = (typeof(this._options.btn) !== 'undefined')? this._options.btn : this._defaults.btn;
    if (btn) {
      if (!(btn instanceof jQuery)) 
        btn = $(btn);

      btn.on('click', function () {
        if (!_this.isOpen()) {
          _this.open(true);
        }
        else {
          _this.close();
        }
        $el.focus();
      });
    }

    // Resize emoji menu automatically
    $(window).on('resize', function () {
      if (_this.resize) {
        _this.resize();   // custom resize function
      }
      else {
        // Default resize function: adapt emoji menu to input width
        var _w = $el.css('width');
        $(d).css('width', _w);
        $(menuContainer).css('width', _w);
        $(dinfo).css('width', _w);
      }
    });

    var currentChars = '', currentVal, currentEmoji;

    var filterEmoji = function () {
      // Use emojis after two character are typed
      if (currentChars.length >= 2) {
        // Escape especial characters
        var regex = new RegExp('^([a-zA-Z0-9]|_?)*' + currentChars.replace(/[-[\]{}()*+?.,\\^$|#\s]/g, "\\$&") + '([a-zA-Z0-9]|_?)*');

        var $domMenu = $(d), dom = $domMenu[0];

        var dir = _this._defaults.folder;
        if (_this._options.folder) {
          dir = _this._options.folder;
        }

        // Get emojis that match user input
        var innerHTML = '', ext = (typeof(_this._options.extension))? _this._options.extension : _this._defaults;
        for (var i = 0, l = _this._icons.length; i < l; i++) {
          currentEmoji = _this._icons[i];
          if (regex.test(currentEmoji)) {
            var classActive = '';
            if (innerHTML === '')
              classActive = 'class="active"';   // First emoji set as 'active'

            innerHTML += '<div ' + classActive + '>' + 
                            '<img src="' + dir + currentEmoji + '.' + ext + '" alt="' + currentEmoji + '" />' +
                            '<span>:' + currentEmoji + ':</span>' +
                          '</div>';
          }
        }
        dom.innerHTML = innerHTML;

        $el.data('jemojiContainer', $domMenu.closest('.jemoji-menu')[0]);

        if (dom.innerHTML.length === 0) {
          _this.close();    // No emojis
          currentChars = '';
          menuOpened = false;
        }
        else {
          // Insert emojis on click
          $el.data('jemojiclick').call();

          _this.open();
        }
      }
    };

    //
    // Keydown to detect arrows, esc and backspace
    //
    $el.jemojiKeydown(function (event) {

      // Close menu on ESC
      if (event.which === 27 && _this.isOpen()) {
        _this.close();
        arrowsCursorBegin = arrowsCursorEnd = -1;
        return;
      }

      if (menuOpened) {

        var currentVal = $el.val();

        var $divs = $(d).find('div'), $index = $divs.index($(d).find('div.active'));

        if (_this.isOpen()) {
          if (event.which === 37 || event.which === 38) {
            if (hasnavigation) {
              // Left arrow
              $divs.removeClass('active');
              var selectedDiv
              if ($index > 0)
                selectedDiv = $($divs.get($index - 1))
              else
                selectedDiv = $($divs.get($divs.length - 1))

              selectedDiv.addClass('active');
              selectedDiv.get(0).scrollIntoView()
              selectedDiv.parent()[0].scrollTop -= 10;

              currentEmoji = $(d).find('div.active img').attr('alt');
              if (arrowsCursorBegin === -1)
                arrowsCursorBegin = getCursorPosition();
              if (arrowsCursorEnd === -1)
                arrowsCursorEnd = getCursorPosition();

              currentVal = currentVal.slice(0, currentVal.lastIndexOf(':', arrowsCursorBegin)) + ':' + currentEmoji + ': ' + currentVal.slice(arrowsCursorEnd);
              $el.val(currentVal);

              arrowsCursorEnd = currentVal.indexOf(':', arrowsCursorBegin) + 2;

              selectedEmoji = true;

              currentChars = '';

              event.preventDefault();
            }
            return;
          }

          if (event.which === 39 || event.which === 40 || event.which === 9) {
            if (hasnavigation) {
              // Right arrow
              $divs.removeClass('active');
              var selectedDiv
              if ($index < ($divs.length - 1))
                selectedDiv = $($divs.get($index + 1))
              else
                selectedDiv = $($divs.get(0))

              selectedDiv.addClass('active');
              selectedDiv.get(0).scrollIntoView()
              selectedDiv.parent()[0].scrollTop -= 10;

              currentEmoji = $(d).find('div.active img').attr('alt');
              if (arrowsCursorBegin === -1)
                arrowsCursorBegin = getCursorPosition();
              if (arrowsCursorEnd === -1)
                arrowsCursorEnd = getCursorPosition();

              currentVal = currentVal.slice(0, currentVal.lastIndexOf(':', arrowsCursorBegin)) + ':' + currentEmoji + ': ' + currentVal.slice(arrowsCursorEnd);
              $el.val(currentVal);

              arrowsCursorEnd = currentVal.indexOf(':', arrowsCursorBegin) + 2;

              // Scroll to selected emoji container
              //$(d).scrollTop($(d).find('div.active img').position().top);

              selectedEmoji = true;

              currentChars = '';

              event.preventDefault();
            }
            return;
          }
        }

        // Backspace
        if (event.which === 8) {
          currentChars = currentChars.slice(0, -1);

          var currentVal = $el.val();

          var $divs = $(d).find('div'), $index = $divs.index($(d).find('div.active'));

          // Backspace
          if (event.which === 8) {
            currentChars = currentChars.slice(0, -1);
            filterEmoji();
          }
        }
      }

    });

    //
    // Keypress for rest of keys
    //
    $el.jemojiKeypress(function (event) {

      // Type selected emoji on Enter if menu is opened
      if (event.which === 13 && _this.isOpen()) {
        isFromEmoji = true;
        $el.focus();
        _this.close();
        menuOpened = false;
        currentChars = '';
        arrowsCursorBegin = arrowsCursorEnd = -1;
        if (!selectedEmoji) {
          // Use case: user open menu, so first emoji is already selected; then press Enter
          currentEmoji = $(d).find('div.active img').attr('alt');
          if (arrowsCursorBegin === -1)
            arrowsCursorBegin = getCursorPosition();
          if (arrowsCursorEnd === -1)
            arrowsCursorEnd = getCursorPosition();

          currentVal = $el.val();
          currentVal = currentVal.slice(0, currentVal.lastIndexOf(':', arrowsCursorBegin)) + ':' + currentEmoji + ': ' + currentVal.slice(arrowsCursorEnd);
          $el.val(currentVal);
        }

        $(menuContainer).find('.jemoji-icons').html('');

        event.preventDefault();
        event.stopPropagation();
        return;
      }

      // Open emoji menu on press ':' key
      if (event.which === 58) {
        if (!menuOpened) {
          menuOpened = true;
          currentChars = '';
          arrowsCursorBegin = arrowsCursorEnd = -1;
        }
        return;
      }

      // Menu opened
      if (menuOpened) {

        var currentVal = $el.val();

        var $divs = $(d).find('div'), $index = $divs.index($(d).find('div.active'));

        currentChars += String.fromCharCode(event.which).toLowerCase();

        filterEmoji();
      }
      
    });

  };

  $.fn.jemoji = function(methodOrOptions) {

    var method = (typeof methodOrOptions === 'string') ? methodOrOptions : undefined;

    if (method) {
      var customPlugins = [];

      function getCustomPlugin() {
        var $el = $(this), customPlugin = $el.data('jemoji');
        customPlugins.push(customPlugin);
      }

      this.each(getCustomPlugin);

      var args = (arguments.length > 1) ? Array.prototype.slice.call(arguments, 1) : undefined, results = [];

      function applyMethod(index) {
        var customPlugin = customPlugins[index];

        if (!customPlugin) {
          console.warn('$.jemoji not instantiated yet');
          console.info(this);
          results.push(undefined);
          return;
        }

        if (typeof customPlugin[method] === 'function') {
          var result = customPlugin[method].apply(customPlugin, args);
          results.push(result);
        } else {
          console.warn('Method \'' + method + '\' not defined in $.jemoji');
        }
      }

      this.each(applyMethod);

      return (results.length > 1) ? results : results[0];
    } else {
      var options = (typeof methodOrOptions === 'object') ? methodOrOptions : undefined;

      function init() {
        var $el = $(this), customPlugin = new jemoji($el, options);

        $el.data('jemoji', customPlugin);
      }

      return this.each(init);
    }

  };

})(jQuery);
