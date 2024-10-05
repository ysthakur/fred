
int fact (int i) {
  
  
  int var$4b7137b;
  if (i == 0) {
    
    var$4b7137b = 1;
  } else {
  
    var$4b7137b = i * fact(i - 1);
  }
  int var$a7751c99 = var$4b7137b;

  return var$a7751c99;
}