<?php
# author: Donghyo Min
# filename: match.php

#include common.php for using top() and bottom() function.
include("common.php");

# This function is for <head> part.
top();

# check result is not false/null; 
# else prints error
function check($result, $message) {
   if (!$result) {
      die("SQL error during $message: " . mysql_error());
   }
}

# performs the given query
# If the result is false/null, print error.
# returns the result set 
function do_query($query) {
   # search database for the biggest cities
   $results = mysql_query($query);
   check($results, "mysql_query(\"$query\")");
   return $results;
}

function get_lat_lon($row) {
	$q1 = str_replace(" ", "+", $row[1]);
	$q2 = str_replace(" ", "+", $row[2]);
	$q3 = str_replace(" ", "+", $row[3]);
	$q4 = str_replace(" ", "+", $row[4]);
	$q5 = str_replace(" ", "+", $row[5]);
	$geocode=file_get_contents("http://maps.google.com/maps/api/geocode/json?address=$q1+$q2+$q3+$q4+$q5&sensor=false");
	$output= json_decode($geocode);
	$lat = $output->results[0]->geometry->location->lat;
	$long = $output->results[0]->geometry->location->lng;
	$arr = array("lat" => $lat, "lon" => $long);
	return $arr;
}

function menu_table($query_data, $food_name, $caption){
    $result = do_query($query_data);
    $row = mysql_fetch_array($result);
    if(!$row){
        ?>
        <p id="explain">
            OOOOPS. <?= $food_name ?> isn't in the database.<br />
            Try another food.
        </p>
        <?php
    } else {    
				$location = get_lat_lon($row);
        ?>
        <div id="table_start">
					<p><?= $caption ?></p>
					<table>
							<tr>
									<th>No.</th>
									<th>Restaurant Name</th>
									<th id="menu_name">menu name</th>
									<!-- will add distance feature later 
									<th>Distance</th> -->
									<th>Map</th>
							</tr>
							
							<?php
							$i = 1;
							while ($row) {
									if($i % 2 == 0){
											$zebra = "even";
									} else {
											$zebra = "odd";
									}
									?>
									
									<tr class = "<?= $zebra ?>" >
											<td> <?= $i ?> </td>
											<td> <?= htmlentities($row[1]) ?> </td>
											<td> <?= htmlentities($row[0]) ?> </td>
											<!-- will add distance feature later 
											<td>0.0 mi</td> -->
											<td id="see_map"> 
												<a href="map.php?lat=<?= $location["lat"] ?>&long=<?= $location["lon"] ?>" >
													<img src="http://www.project-fin.org/openmenu/Front/img/map_icon.jpg" alt="map_icon" />
													See On a Map
												</a>
											</td>
									</tr>
									
									<?php
									$i++;
									$row = mysql_fetch_array($result);
							}
							?>
							
					</table>    
        </div>
    <?php
    }
}

if (isset($_REQUEST["menu_name"])) {
    $menu = $_REQUEST["menu_name"];
}

# Check if the connection is made well; else, print error message.
check(mysql_connect("localhost", "dawgsfor_omuser", "454ftw"), "mysql_connect");

# Check if database choosing is made well; else, print error message.
check(mysql_select_db("dawgsfor_openmenu"), "mysql_select_db");

# search box
type_menu();

$cap = "The restaurants that has ".$menu; 


menu_table(
		"SELECT i.name, r.name, r.address, r.city, r.state, r.country " .
		"From restaurants r " .
		"JOIN restaurants_items ri on ri.rid = r.rid " .
		"JOIN items i on i.iid = ri.iid " .
		"WHERE i.name LIKE ". "'%" . $menu . "%'".
		" ORDER BY r.rid DESC;", $menu, $cap
);

# call this function for validator.
bottom();
?>

