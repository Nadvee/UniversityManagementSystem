<?php

// Show the newest primary-key records first whenever Browse opens a table.
$cfg['TablePrimaryKeyOrder'] = 'DESC';
$cfg['Order'] = 'DESC';
$cfg['RememberSorting'] = false;

// Apply the same defaults even when the browser has older user preferences.
$cfg['UserprefsDisallow'] = array_values(array_unique(array_merge(
    $cfg['UserprefsDisallow'] ?? [],
    ['TablePrimaryKeyOrder', 'Order', 'RememberSorting']
)));
