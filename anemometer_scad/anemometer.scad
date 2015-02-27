
$fn = 60;

cup_R = 20;
cup_r = 19;
cup_t = 1.5;

arm_l = 15;
arm_t = 3;

hub_r = 3;
hub_h = 6;

magnet_r = 6;
magnet_h = 5;
magnet_t = 1;

axis_cuff_h = 3;

axis_r = 1.5;

mount_w = 14;
phone_t = 10.1;
mount_t = 2;
mount_tab_l = 20;

bearing_r = 3;
bearing_h = 3;

eps = 1e-3;

//rotate([0, 0, $t*360]) rotor();
//intersection() {
//	printable_rotor(0);
//	translate([-210, 0, 0])
//		cube([400, 400, 400], center=true);
//}
//magnet_holder();
//phone_mount();
assembly();
//washer();

function sqr(x) = x*x;

module assembly() {
	rotate([0, 270, 0]) phone_mount();
	rotate([0, 0, $t*360]) {
		translate([0, 0, 2*(magnet_r+magnet_t)+axis_cuff_h+2+bearing_h+6]) rotor();
		translate([0, 0, magnet_r+magnet_t+1]) rotate([0, 270, 0]) magnet_holder();
	}
	translate([0, 0, (6+bearing_h+2)/2+2*(magnet_r+magnet_t)+axis_cuff_h])
	cylinder(r=axis_r, h=6+bearing_h+2, center=true);
}

module washer() {
	difference() {
		cylinder(r=2.5, h=2, center=true);
		cylinder(r=1.65, h=2+eps, center=true);
	}
}

module phone_mount() {
	difference() {
		translate([-(mount_tab_l+mount_t)/2+eps, 0, 0]) 
			cube([mount_tab_l+mount_t, phone_t+2*mount_t, mount_w], center=true);
		translate([-mount_tab_l/2-mount_t-eps, 0, 0])
			cube([mount_tab_l, phone_t, mount_w+eps], center=true);
	}
	translate([(2*(magnet_r+magnet_t)+axis_cuff_h+2+bearing_h)/2, 0, 0])
	difference() {
		cube([2*(magnet_r+magnet_t)+axis_cuff_h+6+bearing_h, 2*(magnet_r+magnet_t)+4+2*mount_t, mount_w], center=true);
		union() {
			translate([-(bearing_h)/2, 0, 0])
				cube([2*(magnet_r+magnet_t)+axis_cuff_h+6, 2*(magnet_r+magnet_t)+4, mount_w+eps], center=true);
			translate([(2*(magnet_r+magnet_t)+axis_cuff_h+6)/2, 0, 0]) rotate([0, 90, 0]) 
				#cylinder(r=bearing_r, h=bearing_h+eps, center=true);
		}
	}
	translate([-mount_t/2, 0, 0])
	 cube([mount_t, 2*(magnet_r+magnet_t)+2+2*mount_t, mount_w], center=true);
}

module magnet_holder() {
	difference() {
		union() {
			cylinder(r=magnet_r+magnet_t, h=magnet_h, center=true);
			translate([magnet_r+axis_cuff_h/2, 0, 0])
			rotate([0, 90, 0]) difference() {
				cylinder(r=axis_r+magnet_t, h=axis_cuff_h, center=true);
				cylinder(r=axis_r, h=axis_cuff_h+eps, center=true);
			}
		}
		#cylinder(r=magnet_r, h=magnet_h+eps, center=true);
	}
}

module printable_rotor (top=0) {
	mirror([0, 0, (1-top)])
	intersection() {
		rotor();
		if (top) {
			translate([0, 0, 1e3/2-arm_t/2]) 
				cube(1e3, center=true);
		}
		else {
			translate([0, 0, -1e3/2-arm_t/2-eps])
				cube(1e3, center=true);
		}
	}
}

module rotor() {
	difference() { 
		union() {
			cylinder(r=hub_r, h=hub_h, center=true);
			for (i=[0, 120, 240]) 
				rotate([90, 0, i]) 
					translate([-arm_l-cup_r, 0, -arm_t/2]) union() {
						cup();	
						translate([arm_l/2+cup_r, 0, arm_t/2])
							cube([arm_l+cup_t*1.5, arm_t, arm_t], center=true);
					}
		}
		translate([0, 0, -hub_h/2])
			cylinder(r=axis_r, h=0.8*hub_h+eps);
	}
}	

module cup() {
	translate([0, 0, -sqrt(sqr(cup_R)-sqr(cup_r))]) difference() {
		difference() {
			sphere(r=cup_R);
			sphere(r=cup_R-cup_t);
		}
		translate([0, 0, -cup_R+sqrt(sqr(cup_R)-sqr(cup_r))])
			cube([2*cup_R+eps, 2*cup_R+eps, 2*cup_R], center=true);
	}
}