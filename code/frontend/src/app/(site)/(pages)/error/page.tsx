
import Error from "@/components/Error";

import { Metadata } from "next";
export const metadata: Metadata = {
  title: "Error Page | PandaStore",
  description: "This is Error Page for PandaStore",
  // other metadata
};

const ErrorPage = () => {
  
  return (
    <main>
      <Error />
    </main>
  );
};

export default ErrorPage;
